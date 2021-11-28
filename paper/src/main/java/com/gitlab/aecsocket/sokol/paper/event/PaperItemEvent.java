package com.gitlab.aecsocket.sokol.paper.event;

import com.gitlab.aecsocket.minecommons.core.event.Cancellable;
import com.gitlab.aecsocket.minecommons.core.scheduler.TaskContext;
import com.gitlab.aecsocket.sokol.core.event.ItemEvent;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import com.gitlab.aecsocket.sokol.paper.impl.PaperNode;
import com.gitlab.aecsocket.sokol.paper.wrapper.PaperItem;
import com.gitlab.aecsocket.sokol.paper.wrapper.slot.PaperItemSlot;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.PaperUser;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.InventoryView;

public interface PaperItemEvent extends ItemEvent<PaperNode, PaperItem> {
    @Override PaperUser user();
    @Override PaperItemSlot slot();

    class Base implements PaperItemEvent {
        private final PaperNode node;
        private final PaperUser user;
        private final PaperItemSlot slot;
        private final PaperItem item;

        public Base(PaperNode node, PaperUser user, PaperItemSlot slot, PaperItem item) {
            this.node = node;
            this.user = user;
            this.slot = slot;
            this.item = item;
        }

        @Override public PaperNode node() { return node; }
        @Override public PaperUser user() { return user; }
        @Override public PaperItemSlot slot() { return slot; }
        @Override public PaperItem item() { return item; }
    }

    class BaseCancellable extends Base implements Cancellable {
        private boolean cancelled;

        public BaseCancellable(PaperNode node, PaperUser user, PaperItemSlot slot, PaperItem item) {
            super(node, user, slot, item);
        }

        @Override public boolean cancelled() { return cancelled; }
        @Override public void cancelled(boolean cancelled) { this.cancelled = cancelled; }
    }

    class Hold extends Base
        implements ItemEvent.Hold<PaperNode, PaperItem> {
        private final boolean sync;
        private final TaskContext context;

        public Hold(PaperNode node, PaperUser user, PaperItemSlot slot, PaperItem item, boolean sync, TaskContext context) {
            super(node, user, slot, item);
            this.sync = sync;
            this.context = context;
        }

        @Override public boolean sync() { return sync; }
        @Override public TaskContext context() { return context; }
    }

    interface HandledEvent<E extends Event> extends PaperItemEvent {
        E handle();
    }

    abstract class AbstractHandled<E extends Event> extends Base implements HandledEvent<E> {
        protected final E handle;

        public AbstractHandled(PaperNode node, PaperUser user, PaperItemSlot slot, PaperItem item, E handle) {
            super(node, user, slot, item);
            this.handle = handle;
        }

        @Override public E handle() { return handle; }
    }

    abstract class AbstractHandledCancellable<E extends Event & org.bukkit.event.Cancellable> extends AbstractHandled<E> implements Cancellable {
        public AbstractHandledCancellable(PaperNode node, PaperUser user, PaperItemSlot slot, PaperItem item, E handle) {
            super(node, user, slot, item, handle);
        }

        @Override public boolean cancelled() { return handle.isCancelled(); }
        @Override public void cancelled(boolean cancelled) { handle.setCancelled(cancelled); }
    }

    class Inventory<E extends InventoryEvent & org.bukkit.event.Cancellable> extends AbstractHandledCancellable<E> implements ItemEvent.Inventory<PaperNode, PaperItem> {
        private final InventoryPosition inventoryPosition;

        public Inventory(PaperNode node, PaperUser user, PaperItemSlot slot, PaperItem item, E handle, InventoryPosition inventoryPosition) {
            super(node, user, slot, item, handle);
            this.inventoryPosition = inventoryPosition;
        }

        public static InventoryPosition inventoryPosition(InventoryView view, org.bukkit.inventory.Inventory clicked) {
            return clicked == view.getTopInventory() ? InventoryPosition.TOP : InventoryPosition.BOTTOM;
        }

        @Override public InventoryPosition inventoryPosition() { return inventoryPosition; }
    }

    class InventoryClick extends Inventory<InventoryClickEvent> implements ItemEvent.InventoryClick<PaperNode, PaperItem> {
        public InventoryClick(PaperNode node, PaperUser user, PaperItemSlot slot, PaperItem item, InventoryClickEvent handle, InventoryPosition inventoryPosition) {
            super(node, user, slot, item, handle, inventoryPosition);
        }

        @Override public boolean left() { return handle.isLeftClick(); }
        @Override public boolean right() { return handle.isRightClick(); }
        @Override public boolean shift() { return handle.isShiftClick(); }
    }

    final class SlotClick extends InventoryClick implements ItemEvent.SlotClick<PaperNode, PaperItem> {
        private final PaperItemSlot cursor;

        public SlotClick(PaperNode node, PaperUser user, PaperItemSlot slot, PaperItem item, InventoryClickEvent handle, InventoryPosition inventoryPosition, PaperItemSlot cursor) {
            super(node, user, slot, item, handle, inventoryPosition);
            this.cursor = cursor;
        }

        public static PaperItemEvent.SlotClick of(SokolPlugin plugin, PaperNode node, PaperUser user, PaperItem item, InventoryClickEvent handle) {
            InventoryView view = handle.getView();
            return new PaperItemEvent.SlotClick(node, user,
                    PaperItemSlot.slot(plugin, handle::getCurrentItem, handle::setCurrentItem), item, handle,
                    inventoryPosition(view, handle.getClickedInventory()),
                    PaperItemSlot.slot(plugin, view::getCursor, view::setCursor));
        }

        @Override public PaperItemSlot cursor() { return cursor; }
    }

    final class CursorClick extends InventoryClick implements ItemEvent.SlotClick<PaperNode, PaperItem> {
        private final PaperItemSlot clicked;

        public CursorClick(PaperNode node, PaperUser user, PaperItemSlot slot, PaperItem item, InventoryClickEvent handle, InventoryPosition inventoryPosition, PaperItemSlot clicked) {
            super(node, user, slot, item, handle, inventoryPosition);
            this.clicked = clicked;
        }

        public static PaperItemEvent.CursorClick of(SokolPlugin plugin, PaperNode node, PaperUser user, PaperItem item, InventoryClickEvent handle) {
            InventoryView view = handle.getView();
            return new PaperItemEvent.CursorClick(node, user,
                    PaperItemSlot.slot(plugin, view::getCursor, view::setCursor), item, handle,
                    inventoryPosition(view, handle.getClickedInventory()),
                    PaperItemSlot.slot(plugin, handle::getCurrentItem, handle::setCurrentItem));
        }

        @Override public PaperItemSlot cursor() { return clicked; }
    }

    final class SlotDrag extends Inventory<InventoryDragEvent> implements ItemEvent.SlotDrag<PaperNode, PaperItem> {
        private final int rawSlot;

        public SlotDrag(PaperNode node, PaperUser user, PaperItemSlot slot, PaperItem item, InventoryDragEvent handle, InventoryPosition inventoryPosition, int rawSlot) {
            super(node, user, slot, item, handle, inventoryPosition);
            this.rawSlot = rawSlot;
        }

        @Override public int rawSlot() { return rawSlot; }

        public static PaperItemEvent.SlotDrag of(SokolPlugin plugin, PaperNode node, PaperUser user, PaperItem item, int rawSlot, InventoryDragEvent handle) {
            InventoryView view = handle.getView();
            return new PaperItemEvent.SlotDrag(node, user,
                    PaperItemSlot.slot(plugin, () -> view.getItem(rawSlot), s -> view.setItem(rawSlot, s)),
                    item, handle,
                    inventoryPosition(view, view.getInventory(rawSlot)), rawSlot);
        }
    }
}
