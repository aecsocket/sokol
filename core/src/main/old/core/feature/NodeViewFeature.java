package com.gitlab.aecsocket.sokol.core.feature;

import com.gitlab.aecsocket.minecommons.core.effect.SoundEffect;
import com.gitlab.aecsocket.minecommons.core.event.Cancellable;
import com.gitlab.aecsocket.minecommons.core.translation.Localizer;
import com.gitlab.aecsocket.sokol.core.Node;
import com.gitlab.aecsocket.sokol.core.Tree;
import com.gitlab.aecsocket.sokol.core.event.FeatureEvent;
import com.gitlab.aecsocket.sokol.core.event.ItemEvent;
import com.gitlab.aecsocket.sokol.core.impl.AbstractFeature;
import com.gitlab.aecsocket.sokol.core.node.IncompatibilityException;
import com.gitlab.aecsocket.sokol.core.nodeview.NodeView;
import com.gitlab.aecsocket.sokol.core.stat.StatIntermediate;
import com.gitlab.aecsocket.sokol.core.wrapper.Item;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemUser;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public abstract class NodeViewFeature<F extends NodeViewFeature<F, N, I>.Instance, N extends Node.Scoped<N, I, ?, ?>, I extends Item.Scoped<I, N>>
        extends AbstractFeature<F, N, I> {
    public static final String ID = "node_view";
    public static final String KEY_SLOT_INSERT_SOUND = "slot_insert_sound";
    public static final String KEY_SLOT_REMOVE_SOUND = "slot_remove_sound";
    public static final String KEY_SLOT_COMBINE_SOUND = "slot_combine_sound";

    protected final int listenerPriority;
    protected final NodeView.Options options;
    protected final NodeView.Options combine;

    public NodeViewFeature(int listenerPriority, NodeView.Options options, NodeView.Options combine) {
        this.listenerPriority = listenerPriority;
        this.options = options;
        this.combine = combine;
    }

    public int listenerPriority() { return listenerPriority; }
    public NodeView.Options options() { return options; }
    public NodeView.Options combine() { return combine; }

    public abstract class Instance extends AbstractInstance<N> {
        public Instance(N parent) {
            super(parent);
        }

        @Override public NodeViewFeature<F, N, I> type() { return NodeViewFeature.this; }

        @Override
        public void build(Tree<N> treeCtx, StatIntermediate stats) {
            super.build(treeCtx, stats);
            var events = treeCtx.events();
            events.register(new TypeToken<>() {}, this::onSlotClick, listenerPriority);
            events.register(new TypeToken<>() {}, this::onSlotDrag, listenerPriority);
            events.register(new TypeToken<>() {}, this::onCombineOntoParent, listenerPriority);
            events.register(new TypeToken<>() {}, this::onInsertInto, listenerPriority);
            events.register(new TypeToken<>() {}, this::onRemoveFrom, listenerPriority);
        }

        protected abstract boolean shouldCancel(ItemEvent.SlotClick<N, I> event);
        protected abstract boolean shouldCancel(ItemEvent.SlotDrag<N, I> event);
        protected abstract void openNodeView(ItemEvent.SlotClick<N, I> event, int amount);

        protected abstract Events.@Nullable CombineChildOntoN<N, I, F> createCombineChildOntoN(ItemEvent.SlotClick<N, I> event, Tree<N> parent, Tree<N> child);
        protected abstract Events.@Nullable CombineNOntoParent<N, I, F> createCombineNOntoParent(ItemEvent.SlotClick<N, I> event, Tree<N> parent, Tree<N> child);

        private void onSlotClick(ItemEvent.SlotClick<N, I> event) {
            if (!parent.isRoot()) return;
            if (event.cancelled()) return;

            // cancel interactions on this item if it is the item being viewed in a node tree
            if (shouldCancel(event)) {
                event.cancel();
                return;
            }

            ItemUser user = event.user();
            var tree = event.tree();
            I clicked = event.item();
            event.cursor().get().ifPresentOrElse(cursorItem -> {
                if (!event.left() || !combine.modifiable())
                    return;
                cursorItem.node().ifPresent(cursorNode -> {
                    var cursor = cursorNode.build(user);
                    N parent = combine(event, tree, cursor);
                    if (parent == null)
                        return;
                    event.cancel();

                    int amtCursor = cursorItem.amount();
                    int amtClicked = clicked.amount();
                    if (amtCursor >= amtClicked) {
                        // TODO UPDATE
                        event.slot().set(tree.root().createItem(user).amount(amtClicked));
                        cursorItem.subtract(amtClicked);
                    } else {
                        event.cursor().set(cursorNode.createItem(user).amount(amtCursor));
                        clicked.subtract(amtCursor);
                    }
                });
            }, () -> {
                // no cursor item, open node view
                if (event.right()) {
                    // TODO I need this to fix some sort of desync, but there might be an alternative solution
                    event.cursor().set(null);
                    event.cancel();
                    openNodeView(event, clicked.amount());
                }
            });
        }

        private @Nullable N combine(ItemEvent.SlotClick<N, I> event, Tree<N> parent, Tree<N> child) {
            for (var entry : this.parent.value().slots().entrySet()) {
                var slot = entry.getValue();
                if (combine.limited() && !NodeView.modifiable(slot))
                    continue;
                try {
                    slot.compatibility(parent, child);
                    if (
                            callCancelled(parent, createCombineChildOntoN(event, parent, child))
                            | callCancelled(child, createCombineNOntoParent(event, parent, child))
                    ) continue;
                    parent.node(entry.getKey(), child);
                } catch (IncompatibilityException e) {
                    continue;
                }
                return this.parent;
            }
            for (var sub : this.parent.nodes().values())  {
                N result = combine(event, parent, child);
                if (result != null)
                    return result;
            }
            return null;
        }

        private void onSlotDrag(ItemEvent.SlotDrag<N, I> event) {
            if (!parent.isRoot()) return;
            if (event.cancelled()) return;

            if (event.inventoryPosition() == ItemEvent.InventoryPosition.BOTTOM && shouldCancel(event))
                event.cancel();
        }

        private void onCombineOntoParent(Events.CombineNOntoParent<N, I, F> event) {
            if (!parent.isRoot()) return;
            if (event.cancelled()) return;

            ItemUser user = event.user();
            event.tree().stats().<List<? extends SoundEffect>>value(KEY_SLOT_COMBINE_SOUND).ifPresent(sounds -> {
                sounds.forEach(sound -> user.play(sound, user.position()));
            });
        }

        private void onInsertInto(NodeView.Events.InsertInto<?, N, I> event) {
            if (!parent.isRoot()) return;
            if (event.cancelled()) return;

            ItemUser user = event.user();
            event.tree().stats().<List<? extends SoundEffect>>value(KEY_SLOT_INSERT_SOUND).ifPresent(sounds -> {
                sounds.forEach(sound -> user.play(sound, user.position()));
            });
        }

        private void onRemoveFrom(NodeView.Events.RemoveFrom<?, N, I> event) {
            if (!parent.isRoot()) return;
            if (event.cancelled()) return;

            ItemUser user = event.user();
            event.tree().stats().<List<? extends SoundEffect>>value(KEY_SLOT_REMOVE_SOUND).ifPresent(sounds -> {
                sounds.forEach(sound -> user.play(sound, user.position()));
            });
        }
    }

    @Override public String id() { return ID; }

    // todo
    @Override
    public Optional<List<Component>> renderConfig(Locale locale, Localizer lc) { return Optional.empty(); }

    public static final class Events {
        private Events() {}

        public interface CombineChildOntoN<N extends Node.Scoped<N, I, ?, ?>, I extends Item.Scoped<I, N>, F extends NodeViewFeature<F, N, I>.Instance>
                extends FeatureEvent<N, F>, ItemEvent<N, I>, Cancellable {
            N child();
        }

        public interface CombineNOntoParent<N extends Node.Scoped<N, I, ?, ?>, I extends Item.Scoped<I, N>, F extends NodeViewFeature<F, N, I>.Instance>
                extends FeatureEvent<N, F>, ItemEvent<N, I>, Cancellable {
            N parent();
        }
    }
}
