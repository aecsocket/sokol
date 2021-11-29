package com.gitlab.aecsocket.sokol.core.feature;

import com.gitlab.aecsocket.minecommons.core.Quantifier;
import com.gitlab.aecsocket.minecommons.core.translation.Localizer;
import com.gitlab.aecsocket.sokol.core.Node;
import com.gitlab.aecsocket.sokol.core.TreeContext;
import com.gitlab.aecsocket.sokol.core.event.CreateItemEvent;
import com.gitlab.aecsocket.sokol.core.event.ItemEvent;
import com.gitlab.aecsocket.sokol.core.impl.AbstractFeature;
import com.gitlab.aecsocket.sokol.core.node.IncompatibilityException;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.stat.StatIntermediate;
import com.gitlab.aecsocket.sokol.core.wrapper.Item;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemUser;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;

public abstract class NodeHolderFeature<F extends NodeHolderFeature<F, N, I>.Instance, N extends Node.Scoped<N, I, ?, ?>, I extends Item.Scoped<I, N>>
        extends AbstractFeature<F, N, I> {
    public static final String ID = "node_holder";

    public enum Position {
        TOP,
        BOTTOM
    }

    protected final int listenerPriority;
    protected Rule rule;
    protected final Position headerPosition;
    protected final @Nullable Integer capacity;
    protected final boolean showFullAsDurability;

    public NodeHolderFeature(int listenerPriority, Rule rule, Position headerPosition, @Nullable Integer capacity, boolean showFullAsDurability) {
        this.listenerPriority = listenerPriority;
        this.rule = rule;
        this.headerPosition = headerPosition;
        this.capacity = capacity;
        this.showFullAsDurability = showFullAsDurability;
    }

    public int listenerPriority() { return listenerPriority; }
    public Rule rule() { return rule; }
    public Position headerPosition() { return headerPosition; }
    public Integer capacity() { return capacity; }
    public boolean showFullAsDurability() { return showFullAsDurability; }

    public abstract class Instance extends AbstractInstance<N> {
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

        protected abstract TypeToken<? extends CreateItemEvent<N, I>> eventCreateItem();
        protected abstract TypeToken<? extends ItemEvent.SlotClick<N, I>> eventSlotClick();

        @Override
        public void build(TreeContext<N> treeCtx, StatIntermediate stats) {
            super.build(treeCtx, stats);
            var events = treeCtx.events();
            events.register(eventCreateItem(), this::onCreateItem, listenerPriority);
            events.register(eventSlotClick(), this::onSlotClick, listenerPriority);
        }

        private void onCreateItem(CreateItemEvent<N, I> event) {
            if (!parent.isRoot()) return;
            Locale locale = event.locale();
            N node = event.node();
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
            N node = event.node();
            ItemUser user = event.user();
            event.cursor().get().ifPresentOrElse(cursor -> {
                if (!event.left())
                    return;
                event.cancel();
                int rAmount = event.shift() ? 1 : cursor.amount();
                int amount = capacity == null ? rAmount : Math.min(capacity - size(), rAmount);
                if (amount <= 0)
                    return;
                cursor.node().ifPresent(cursorNode -> {
                    try {
                        rule.applies(cursorNode, treeCtx);
                    } catch (IncompatibilityException e) {
                        // TODO send msg
                        return;
                    }
                    add(cursorNode, amount);
                    cursor.subtract(amount);
                    // TODO update meth THIS IGNORES THE EXISTING ITEM STACK AMOUNT OMG FIX
                    event.slot().set(node.createItem(user));
                });
            }, () -> {
                if (!event.right() || !event.shift())
                    return;
                event.cancel();
                if (nodes.isEmpty())
                    return;
                var last = nodes.removeLast();
                event.cursor().set(last.object().createItem(user).amount(last.amount()));
                // TODO update meth THIS IGNORES THE EXISTING ITEM STACK AMOUNT OMG FIX
                event.slot().set(node.createItem(user));
            });
        }
    }

    @Override public String id() { return ID; }

    // todo
    @Override
    public Optional<List<Component>> renderConfig(Locale locale, Localizer lc) {
        return Optional.empty();
    }
}
