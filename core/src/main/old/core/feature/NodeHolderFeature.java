package com.gitlab.aecsocket.sokol.core.feature;

import com.gitlab.aecsocket.minecommons.core.Quantifier;
import com.gitlab.aecsocket.minecommons.core.effect.SoundEffect;
import com.gitlab.aecsocket.minecommons.core.event.Cancellable;
import com.gitlab.aecsocket.minecommons.core.translation.Localizer;
import com.gitlab.aecsocket.sokol.core.Node;
import com.gitlab.aecsocket.sokol.core.Tree;
import com.gitlab.aecsocket.sokol.core.event.CreateItemEvent;
import com.gitlab.aecsocket.sokol.core.event.FeatureEvent;
import com.gitlab.aecsocket.sokol.core.event.ItemEvent;
import com.gitlab.aecsocket.sokol.core.impl.AbstractFeature;
import com.gitlab.aecsocket.sokol.core.node.IncompatibilityException;
import com.gitlab.aecsocket.sokol.core.stat.StatIntermediate;
import com.gitlab.aecsocket.sokol.core.wrapper.Item;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemSlot;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemUser;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;

public abstract class NodeHolderFeature<F extends NodeHolderFeature<F, N, I>.Instance, N extends Node.Scoped<N, I, ?, ?, ?>, I extends Item.Scoped<I, N>>
        extends AbstractFeature<Void, F, N, I> {
    public static final String ID = "node_holder";
    public static final String KEY_NODE_ADD_SOUND = "node_add_sound";
    public static final String KEY_NODE_REMOVE_SOUND = "node_remove_sound";

    public enum Position {
        TOP,
        BOTTOM
    }

    protected final int listenerPriority;
    protected Rule rule;
    protected final Position headerPosition;
    protected final @Nullable Integer capacity;
    protected final boolean showFullAsDurability;
    protected final boolean nodeViewOverridesSound;

    public NodeHolderFeature(int listenerPriority, Position headerPosition, @Nullable Integer capacity, boolean showFullAsDurability, boolean nodeViewOverridesSound) {
        this.listenerPriority = listenerPriority;
        this.headerPosition = headerPosition;
        this.capacity = capacity;
        this.showFullAsDurability = showFullAsDurability;
        this.nodeViewOverridesSound = nodeViewOverridesSound;
    }

    public int listenerPriority() { return listenerPriority; }
    public Rule rule() { return rule; }
    public Position headerPosition() { return headerPosition; }
    public Integer capacity() { return capacity; }
    public boolean showFullAsDurability() { return showFullAsDurability; }
    public boolean nodeViewOverridesSound() { return nodeViewOverridesSound; }

    public abstract class Instance extends AbstractInstance<Void, N> {
        protected final LinkedList<Quantifier<N>> nodes;

        public Instance(N parent, LinkedList<Quantifier<N>> nodes) {
            super(parent);
            this.nodes = nodes;
        }

        public Instance(N parent, Instance o) {
            super(parent);
            nodes = new LinkedList<>();
            for (var qt : o.nodes)
                nodes.add(new Quantifier<>(qt.object().copy(), qt.amount()));
        }

        public Instance(N parent) {
            super(parent);
            nodes = new LinkedList<>();
        }

        @Override public NodeHolderFeature<F, N, I> type() { return NodeHolderFeature.this; }

        public LinkedList<Quantifier<N>> nodes() { return nodes; }

        protected abstract boolean equal(N a, N b);

        public int size() {
            return Quantifier.total(nodes);
        }

        public void add(N node, int amount) {
            if (nodes.isEmpty())
                nodes.add(new Quantifier<>(node, amount));
            else {
                int i = nodes.size() - 1;
                var last = nodes.get(i);
                if (equal(last.object(), node))
                    nodes.set(i, last.add(amount));
                else
                    nodes.add(new Quantifier<>(node, amount));
            }
        }

        public Optional<N> peek() {
            return nodes.isEmpty() ? Optional.empty() : Optional.of(nodes.peekLast().object());
        }

        public Optional<N> pop() {
            return nodes.isEmpty() ? Optional.empty() : Optional.of(nodes.removeLast().object());
        }

        @Override
        public void build(Tree<N> treeCtx, StatIntermediate stats) {
            super.build(treeCtx, stats);
            var events = treeCtx.events();
            events.register(new TypeToken<>() {}, this::onCreateItem, listenerPriority);
            events.register(new TypeToken<>() {}, this::onSlotClick, listenerPriority);
            events.register(new TypeToken<>() {}, this::onNodeAdd, listenerPriority);
            events.register(new TypeToken<>() {}, this::onNodeRemove, listenerPriority);
        }

        protected abstract Events.@Nullable NodeAdd<N, I, F> createNodeAdd(ItemEvent.SlotClick<N, I> event, Tree<N> held, I heldItem);
        protected abstract Events.@Nullable NodeRemove<N, I, F> createNodeRemove(ItemEvent.SlotClick<N, I> event, Tree<N> remove);

        private void onCreateItem(CreateItemEvent<N, I> event) {
            if (!parent.isRoot()) return;
            Locale locale = event.locale();
            var tree = event.tree();
            List<Component> lines = new ArrayList<>();

            int total = size();
            List<Component> header = platform().lc().lines(locale, lcKey("header." + (total == 0 ? "empty"
                    : capacity != null && total >= capacity ? "full" : "default")),
                            "total", ""+total,
                            "capacity", ""+capacity)
                            .orElse(null);

            if (header != null && headerPosition == Position.TOP)
                lines.addAll(header);

            for (var qt : nodes) {
                platform().lc().lines(locale, lcKey("entry"),
                        "amount", ""+qt.amount(),
                        "value", qt.object().value().render(locale, platform().lc()))
                        .ifPresent(lines::addAll);
            }

            if (header != null && headerPosition == Position.BOTTOM)
                lines.addAll(header);

            event.item().addDescription(lines);

            if (capacity != null && showFullAsDurability)
                event.item().durability((double) total / capacity);
        }

        private void onSlotClick(ItemEvent.SlotClick<N, I> event) {
            if (!parent.isRoot()) return;
            if (event.cancelled()) return;
            var tree = event.tree();
            ItemUser user = event.user();
            event.cursor().get().ifPresentOrElse(cursorItem -> {
                if (!event.left())
                    return;
                event.cancel();
                int rAmount = event.shift() ? 1 : cursorItem.amount();
                int amount = capacity == null ? rAmount : Math.min(capacity - size(), rAmount);
                if (amount <= 0)
                    return;
                cursorItem.node().ifPresent(cursorNode -> {
                    var cursor = cursorNode.build(user);
                    try {
                        rule.applies(cursorNode, new CompatibilityTree<>(tree, cursor));
                    } catch (IncompatibilityException e) {
                        // TODO send msg
                        return;
                    }
                    if (callCancelled(tree, createNodeAdd(event, cursor, cursorItem)))
                        return;
                    add(cursorNode, amount);
                    cursorItem.subtract(amount);
                    // TODO update meth THIS IGNORES THE EXISTING ITEM STACK AMOUNT OMG FIX
                    event.slot().set(tree.root().createItem(user));
                });
            }, () -> {
                if (!event.right() || !event.shift())
                    return;
                event.cancel();
                if (nodes.isEmpty())
                    return;
                var last = nodes.getLast();
                N removeNode = last.object();
                Tree<N> remove = removeNode.build(user);
                if (callCancelled(tree, createNodeRemove(event, remove)))
                    return;
                nodes.removeLast();
                event.cursor().set(removeNode.createItem(user).amount(last.amount()));
                // TODO update meth THIS IGNORES THE EXISTING ITEM STACK AMOUNT OMG FIX
                event.slot().set(tree.root().createItem(user));
            });
        }

        // TODO merge make better
        private void onNodeAdd(Events.NodeAdd<N, I, F> event) {
            if (!parent.isRoot()) return;
            if (event.cancelled()) return;

            ItemUser user = event.user();
            var tree = event.tree();
            (nodeViewOverridesSound
                    ? event.held().stats().<List<? extends SoundEffect>>value(NodeViewFeature.KEY_SLOT_INSERT_SOUND)
                    : Optional.<List<? extends SoundEffect>>empty())
                    .or(() -> tree.stats().value(KEY_NODE_ADD_SOUND))
                    .ifPresent(sounds -> sounds.forEach(sound -> user.play(sound, user.position())));
        }

        private void onNodeRemove(Events.NodeRemove<N, I, F> event) {
            if (!parent.isRoot()) return;
            if (event.cancelled()) return;

            ItemUser user = event.user();
            var tree = event.tree();
            (nodeViewOverridesSound
                    ? event.held().stats().<List<? extends SoundEffect>>value(NodeViewFeature.KEY_SLOT_REMOVE_SOUND)
                    : Optional.<List<? extends SoundEffect>>empty())
                    .or(() -> tree.stats().value(KEY_NODE_REMOVE_SOUND))
                    .ifPresent(sounds -> sounds.forEach(sound -> user.play(sound, user.position())));
        }
    }

    @Override public String id() { return ID; }

    // todo
    @Override
    public Optional<List<Component>> renderConfig(Locale locale, Localizer lc) {
        return Optional.empty();
    }

    public static final class Events {
        private Events() {}

        public interface NodeModify<N extends Node.Scoped<N, I, ?, ?>, I extends Item.Scoped<I, N>, F extends NodeHolderFeature<F, N, I>.Instance>
                extends FeatureEvent<N, F>, ItemEvent<N, I>, Cancellable {
            Tree<N> held();
            ItemSlot<I> cursor();
        }

        public interface NodeAdd<N extends Node.Scoped<N, I, ?, ?>, I extends Item.Scoped<I, N>, F extends NodeHolderFeature<F, N, I>.Instance>
                extends NodeModify<N, I, F> {
            I heldItem();
        }

        public interface NodeRemove<N extends Node.Scoped<N, I, ?, ?>, I extends Item.Scoped<I, N>, F extends NodeHolderFeature<F, N, I>.Instance>
                extends NodeModify<N, I, F> {}
    }
}
