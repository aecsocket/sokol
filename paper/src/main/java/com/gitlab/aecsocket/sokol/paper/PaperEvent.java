package com.gitlab.aecsocket.sokol.paper;

import com.gitlab.aecsocket.minecommons.core.InputType;
import com.gitlab.aecsocket.minecommons.paper.inputs.Inputs;
import com.gitlab.aecsocket.sokol.core.tree.event.ItemTreeEvent;
import com.gitlab.aecsocket.sokol.core.tree.event.TreeEvent;
import com.gitlab.aecsocket.sokol.paper.wrapper.slot.PaperSlot;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.LivingEntityUser;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.PlayerUser;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryClickEvent;

import static com.gitlab.aecsocket.sokol.paper.wrapper.user.PaperUser.*;

public interface PaperEvent extends TreeEvent.ItemEvent {
    @Override PaperTreeNode node();

    interface FromServer extends PaperEvent {
        Event handle();
    }

    final class Holding implements ItemTreeEvent.Holding, PaperEvent {
        private final PaperTreeNode node;
        private final PlayerUser user;
        private final PaperSlot slot;
        private final boolean sync;
        private final long elapsed;
        private final long delta;
        private final int iteration;

        public Holding(PaperTreeNode node, PlayerUser user, PaperSlot slot, boolean sync, long elapsed, long delta, int iteration) {
            this.node = node;
            this.user = user;
            this.slot = slot;
            this.sync = sync;
            this.elapsed = elapsed;
            this.delta = delta;
            this.iteration = iteration;
        }

        @Override public PaperTreeNode node() { return node; }
        @Override public PlayerUser user() { return user; }
        @Override public PaperSlot slot() { return slot; }
        @Override public boolean sync() { return sync; }
        @Override public long elapsed() { return elapsed; }
        @Override public long delta() { return delta; }
        @Override public int iteration() { return iteration; }
    }

    final class ClickedSlotClickEvent implements ItemTreeEvent.ClickedSlotClickEvent, FromServer {
        private final InventoryClickEvent handle;
        private final PaperTreeNode node;
        private final LivingEntityUser user;
        private final PaperSlot slot;
        private final PaperSlot cursor;

        public ClickedSlotClickEvent(SokolPlugin plugin, InventoryClickEvent handle, PaperTreeNode node) {
            this.handle = handle;
            this.node = node;
            user = living(plugin, handle.getWhoClicked());
            slot = PaperSlot.slot(plugin, handle::getCurrentItem, handle::setCurrentItem);
            cursor = PaperSlot.slot(plugin, handle.getView()::getCursor, handle.getView()::setCursor);
        }

        @Override public InventoryClickEvent handle() { return handle; }
        @Override public PaperTreeNode node() { return node; }
        @Override public LivingEntityUser user() { return user; }
        @Override public PaperSlot slot() { return slot; }
        @Override public PaperSlot cursor() { return cursor; }

        @Override public boolean cancelled() { return handle.isCancelled(); }
        @Override public void cancelled(boolean cancelled) { handle.setCancelled(cancelled); }

        @Override public boolean left() { return handle.isLeftClick(); }
        @Override public boolean right() { return handle.isRightClick(); }
        @Override public boolean shift() { return handle.isShiftClick(); }
    }

    final class CursorSlotClickEvent implements ItemTreeEvent.CursorSlotClickEvent, FromServer {
        private final InventoryClickEvent handle;
        private final PaperTreeNode node;
        private final LivingEntityUser user;
        private final PaperSlot slot;
        private final PaperSlot clicked;

        public CursorSlotClickEvent(SokolPlugin plugin, InventoryClickEvent handle, PaperTreeNode node) {
            this.handle = handle;
            this.node = node;
            user = living(plugin, handle.getWhoClicked());
            slot = PaperSlot.slot(plugin, handle.getView()::getCursor, handle.getView()::setCursor);
            clicked = PaperSlot.slot(plugin, handle::getCurrentItem, handle::setCurrentItem);
        }

        @Override public InventoryClickEvent handle() { return handle; }
        @Override public PaperTreeNode node() { return node; }
        @Override public LivingEntityUser user() { return user; }
        @Override public PaperSlot slot() { return slot; }
        @Override public PaperSlot clicked() { return clicked; }

        @Override public boolean cancelled() { return handle.isCancelled(); }
        @Override public void cancelled(boolean cancelled) { handle.setCancelled(cancelled); }

        @Override public boolean left() { return handle.isLeftClick(); }
        @Override public boolean right() { return handle.isRightClick(); }
        @Override public boolean shift() { return handle.isShiftClick(); }
    }

    final class InputEvent implements ItemTreeEvent.InputEvent, PaperEvent {
        private final PaperTreeNode node;
        private final PlayerUser user;
        private final PaperSlot slot;
        private final Inputs.Events.Input event;
        private boolean cancelled;

        public InputEvent(PaperTreeNode node, PlayerUser user, PaperSlot slot, Inputs.Events.Input event) {
            this.node = node;
            this.user = user;
            this.slot = slot;
            this.event = event;
        }

        @Override public PaperTreeNode node() { return node; }
        @Override public LivingEntityUser user() { return user; }
        @Override public PaperSlot slot() { return slot; }
        @Override public InputType input() { return event.input(); }
        public Inputs.Events.Input event() { return event; }

        @Override public boolean cancelled() { return cancelled; }
        @Override public void cancelled(boolean cancelled) { this.cancelled = cancelled; }
    }

    record RawInputEvent(
            PaperTreeNode node,
            PlayerUser user,
            PaperSlot slot,
            Inputs.Events.Input event
    ) implements TreeEvent.ItemEvent, PaperEvent {}
}
