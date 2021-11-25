package com.gitlab.aecsocket.sokol.paper.event;

import com.gitlab.aecsocket.minecommons.core.event.Cancellable;
import com.gitlab.aecsocket.minecommons.core.scheduler.TaskContext;
import com.gitlab.aecsocket.sokol.core.event.ItemEvent;
import com.gitlab.aecsocket.sokol.paper.impl.PaperNode;
import com.gitlab.aecsocket.sokol.paper.wrapper.PaperItem;
import com.gitlab.aecsocket.sokol.paper.wrapper.slot.PaperItemSlot;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.PaperUser;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryView;

public interface PaperItemEvent extends ItemEvent<PaperNode, PaperItem> {
    @Override PaperUser user();
    @Override PaperItemSlot slot();

    record Hold(
            PaperNode node,
            PaperUser user,
            PaperItemSlot slot,
            PaperItem item,
            boolean sync,
            TaskContext context
    ) implements PaperItemEvent, ItemEvent.Hold<PaperNode, PaperItem> {}

    interface HandledEvent<E extends Event> extends PaperItemEvent {
        E handle();
    }

    abstract class AbstractHandled<E extends Event> implements HandledEvent<E> {
        protected final PaperNode node;
        protected final PaperUser user;
        protected final PaperItemSlot slot;
        protected final PaperItem item;
        protected final E handle;

        public AbstractHandled(PaperNode node, PaperUser user, PaperItemSlot slot, PaperItem item, E handle) {
            this.node = node;
            this.user = user;
            this.slot = slot;
            this.item = item;
            this.handle = handle;
        }

        @Override public PaperNode node() { return node; }
        @Override public PaperUser user() { return user; }
        @Override public PaperItemSlot slot() { return slot; }
        @Override public PaperItem item() { return item; }
        @Override public E handle() { return handle; }
    }

    abstract class AbstractHandledCancellable<E extends Event & org.bukkit.event.Cancellable> extends AbstractHandled<E> implements Cancellable {
        public AbstractHandledCancellable(PaperNode node, PaperUser user, PaperItemSlot slot, PaperItem item, E handle) {
            super(node, user, slot, item, handle);
        }

        @Override public boolean cancelled() { return handle.isCancelled(); }
        @Override public void cancelled(boolean cancelled) { handle.setCancelled(cancelled); }
    }

    class InventoryClick extends AbstractHandledCancellable<InventoryClickEvent> implements ItemEvent.InventoryClick<PaperNode, PaperItem> {
        public InventoryClick(PaperNode node, PaperUser user, PaperItemSlot slot, PaperItem item, InventoryClickEvent handle) {
            super(node, user, slot, item, handle);
        }

        @Override public boolean left() { return handle.isLeftClick(); }
        @Override public boolean right() { return handle.isRightClick(); }
        @Override public boolean shift() { return handle.isShiftClick(); }
    }

    final class SlotClick extends InventoryClick implements ItemEvent.SlotClick<PaperNode, PaperItem> {
        private final PaperItemSlot cursor;

        public SlotClick(PaperNode node, PaperUser user, PaperItemSlot slot, PaperItem item, InventoryClickEvent handle, PaperItemSlot cursor) {
            super(node, user, slot, item, handle);
            this.cursor = cursor;
        }

        public static PaperItemEvent.SlotClick of(PaperNode node, PaperUser user, PaperItem item, InventoryClickEvent handle) {
            InventoryView view = handle.getView();
            return new PaperItemEvent.SlotClick(node, user,
                    PaperItemSlot.slot(handle::getCurrentItem, handle::setCurrentItem), item, handle,
                    PaperItemSlot.slot(view::getCursor, view::setCursor));
        }

        @Override public PaperItemSlot cursor() { return cursor; }
    }

    final class CursorClick extends InventoryClick implements ItemEvent.SlotClick<PaperNode, PaperItem> {
        private final PaperItemSlot clicked;

        public CursorClick(PaperNode node, PaperUser user, PaperItemSlot slot, PaperItem item, InventoryClickEvent handle, PaperItemSlot clicked) {
            super(node, user, slot, item, handle);
            this.clicked = clicked;
        }

        public static PaperItemEvent.CursorClick of(PaperNode node, PaperUser user, PaperItem item, InventoryClickEvent handle) {
            InventoryView view = handle.getView();
            return new PaperItemEvent.CursorClick(node, user,
                    PaperItemSlot.slot(view::getCursor, view::setCursor), item, handle,
                    PaperItemSlot.slot(handle::getCurrentItem, handle::setCurrentItem));
        }

        @Override public PaperItemSlot cursor() { return clicked; }
    }

    final class SlotDrag extends AbstractHandledCancellable<InventoryDragEvent> implements ItemEvent.SlotDrag<PaperNode, PaperItem> {
        private final int rawSlot;

        public SlotDrag(PaperNode node, PaperUser user, PaperItemSlot slot, PaperItem item, InventoryDragEvent handle, int rawSlot) {
            super(node, user, slot, item, handle);
            this.rawSlot = rawSlot;
        }

        @Override public int rawSlot() { return rawSlot; }

        public static PaperItemEvent.SlotDrag of(PaperNode node, PaperUser user, PaperItem item, int rawSlot, InventoryDragEvent handle) {
            return new PaperItemEvent.SlotDrag(node, user,
                    PaperItemSlot.slot(() -> handle.getView().getItem(rawSlot), s -> handle.getView().setItem(rawSlot, s)),
                    item, handle, rawSlot);
        }
    }
}
