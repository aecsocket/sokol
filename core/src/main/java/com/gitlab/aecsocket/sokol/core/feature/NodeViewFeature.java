package com.gitlab.aecsocket.sokol.core.feature;

import com.gitlab.aecsocket.minecommons.core.effect.SoundEffect;
import com.gitlab.aecsocket.minecommons.core.event.Cancellable;
import com.gitlab.aecsocket.minecommons.core.translation.Localizer;
import com.gitlab.aecsocket.sokol.core.Node;
import com.gitlab.aecsocket.sokol.core.TreeContext;
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

        protected abstract TypeToken<? extends ItemEvent.SlotClick<N, I>> eventSlotClick();
        protected abstract TypeToken<? extends ItemEvent.SlotDrag<N, I>> eventSlotDrag();
        protected abstract TypeToken<? extends Events.CombineOntoParent<N, I, F>> eventCombineOntoParent();
        protected abstract TypeToken<? extends NodeView.Events.InsertInto<?, N, I>> eventInsertInto();
        protected abstract TypeToken<? extends NodeView.Events.RemoveFrom<?, N, I>> eventRemoveFrom();

        @Override
        public void build(TreeContext<N> treeCtx, StatIntermediate stats) {
            super.build(treeCtx, stats);
            var events = treeCtx.events();
            events.register(eventSlotClick(), this::onSlotClick, listenerPriority);
            events.register(eventSlotDrag(), this::onSlotDrag, listenerPriority);
            events.register(eventCombineOntoParent(), this::onCombineOntoParent, listenerPriority);
            events.register(eventInsertInto(), this::onInsertInto, listenerPriority);
            events.register(eventRemoveFrom(), this::onRemoveFrom, listenerPriority);
        }

        protected abstract boolean cancelIfClickedViewedItem(ItemEvent.SlotClick<N, I> event);
        protected abstract boolean cancelIfClickedViewedItem(ItemEvent.SlotDrag<N, I> event);
        protected abstract void openNodeView(ItemEvent.SlotClick<N, I> event, int amount);

        protected abstract boolean callCombineOntoParent(ItemEvent.SlotClick<N, I> event, N node, N parent);
        protected abstract boolean callCombineChildOnto(ItemEvent.SlotClick<N, I> event, N node, N child);

        private void onSlotClick(ItemEvent.SlotClick<N, I> event) {
            if (!parent.isRoot()) return;
            if (event.cancelled()) return;

            // cancel interactions on this item if it is the item being viewed in a node tree
            if (cancelIfClickedViewedItem(event)) {
                event.cancel();
                return;
            }

            ItemUser user = event.user();
            N node = event.node();
            I clicked = event.item();
            event.cursor().get().ifPresentOrElse(cursor -> {
                if (!event.left() || !combine.modifiable())
                    return;
                cursor.node().ifPresent(nCursor -> {
                    if (!combine(node.root(), nCursor, treeCtx, nCursor.build(user)))
                        return;
                    event.cancel();
                    if (callCombineOntoParent(event, node, parent) | callCombineChildOnto(event, parent, node))
                        return;

                    int amtCursor = cursor.amount();
                    int amtClicked = clicked.amount();
                    if (amtCursor >= amtClicked) {
                        // TODO UPDATE
                        event.slot().set(node.createItem(user).amount(amtClicked));
                        cursor.subtract(amtClicked);
                    } else {
                        event.cursor().set(node.createItem(user).amount(amtCursor));
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

        private boolean combine(N parent, N child, TreeContext<N> parentCtx, TreeContext<N> childCtx) {
            for (var entry : parent.value().slots().entrySet()) {
                if (combine.limited() && !NodeView.modifiable(entry.getValue()))
                    continue;
                try {
                    parent.node(entry.getKey(), child, parentCtx, childCtx);
                } catch (IncompatibilityException e) {
                    continue;
                }
                return true;
            }
            for (var sub : parent.nodes().values())  {
                if (combine(sub, child, parentCtx, childCtx))
                    return true;
            }
            return false;
        }

        private void onSlotDrag(ItemEvent.SlotDrag<N, I> event) {
            if (!parent.isRoot()) return;
            if (event.cancelled()) return;

            if (event.inventoryPosition() == ItemEvent.InventoryPosition.BOTTOM && cancelIfClickedViewedItem(event))
                event.cancel();
        }

        private void onCombineOntoParent(Events.CombineOntoParent<N, I, F> event) {
            if (!parent.isRoot()) return;
            if (event.cancelled()) return;

            ItemUser user = event.user();
            treeCtx.stats().<List<? extends SoundEffect>>value(KEY_SLOT_COMBINE_SOUND).ifPresent(sounds -> {
                sounds.forEach(sound -> user.play(sound, user.position()));
            });
        }

        private void onInsertInto(NodeView.Events.InsertInto<?, N, I> event) {
            if (!parent.isRoot()) return;
            if (event.cancelled()) return;

            ItemUser user = event.user();
            treeCtx.stats().<List<? extends SoundEffect>>value(KEY_SLOT_INSERT_SOUND).ifPresent(sounds -> {
                sounds.forEach(sound -> user.play(sound, user.position()));
            });
        }

        private void onRemoveFrom(NodeView.Events.RemoveFrom<?, N, I> event) {
            if (!parent.isRoot()) return;
            if (event.cancelled()) return;

            ItemUser user = event.user();
            treeCtx.stats().<List<? extends SoundEffect>>value(KEY_SLOT_REMOVE_SOUND).ifPresent(sounds -> {
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

        public interface CombineOntoParent<N extends Node.Scoped<N, I, ?, ?>, I extends Item.Scoped<I, N>, F extends NodeViewFeature<F, N, I>.Instance>
                extends FeatureEvent<N, F>, ItemEvent<N, I>, Cancellable {
            N parent();
        }

        public interface CombineChildOnto<N extends Node.Scoped<N, I, ?, ?>, I extends Item.Scoped<I, N>, F extends NodeViewFeature<F, N, I>.Instance>
                extends FeatureEvent<N, F>, ItemEvent<N, I>, Cancellable {
            N child();
        }
    }
}
