package com.gitlab.aecsocket.sokol.paper;

import com.gitlab.aecsocket.minecommons.core.InputType;
import com.gitlab.aecsocket.minecommons.paper.inputs.Inputs;
import com.gitlab.aecsocket.sokol.core.tree.event.ItemTreeEvent;
import com.gitlab.aecsocket.sokol.core.tree.event.TreeEvent;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemStack;
import com.gitlab.aecsocket.sokol.paper.wrapper.slot.PaperSlot;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.EntityUser;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.LivingEntityUser;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.PaperUser;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.PlayerUser;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.function.Function;

import static com.gitlab.aecsocket.sokol.paper.wrapper.user.PaperUser.*;

public interface PaperEvent extends TreeEvent.ItemEvent {
    @Override PaperTreeNode node();
    @Override PaperUser user();
    @Override PaperSlot slot();

    interface FromServer extends PaperEvent {
        Event handle();
    }

    abstract class Base implements PaperEvent {
        protected final PaperTreeNode node;
        protected final PaperSlot slot;
        protected Function<ItemStack, ItemStack> updateQueued;

        public Base(PaperTreeNode node, PaperSlot slot) {
            this.node = node;
            this.slot = slot;
        }

        @Override public PaperTreeNode node() { return node; }
        @Override public PaperSlot slot() { return slot; }

        @Override
        public void queueUpdate(@Nullable Function<ItemStack, ItemStack> function) {
            if (updateQueued == null) {
                Function<ItemStack, ItemStack> underlying = is -> is.amount(slot.get()
                        .orElseThrow(() -> new IllegalStateException("Updating slot with no item"))
                        .amount());
                updateQueued = function == null
                        ? underlying
                        : is -> function.apply(underlying.apply(is));
            } else {
                if (function != null) {
                    Function<ItemStack, ItemStack> old = updateQueued;
                    updateQueued = is -> function.apply(old.apply(is));
                }
            }
        }

        @Override
        public boolean call() {
            boolean result = PaperEvent.super.call();
            if (updateQueued != null) {
                node.build();
                forceUpdate(updateQueued);
            }
            return result;
        }
    }

    final class Hold extends Base implements ItemTreeEvent.Hold {
        private final PlayerUser user;
        private final boolean sync;
        private final long elapsed;
        private final long delta;
        private final int iteration;

        public Hold(PaperTreeNode node, PlayerUser user, PaperSlot slot, boolean sync, long elapsed, long delta, int iteration) {
            super(node, slot);
            this.user = user;
            this.sync = sync;
            this.elapsed = elapsed;
            this.delta = delta;
            this.iteration = iteration;
        }

        @Override public PlayerUser user() { return user; }
        @Override public boolean sync() { return sync; }
        @Override public long elapsed() { return elapsed; }
        @Override public long delta() { return delta; }
        @Override public int iteration() { return iteration; }
    }

    final class ClickedSlotClickEvent extends Base implements ItemTreeEvent.ClickedSlotClickEvent, FromServer {
        private final InventoryClickEvent handle;
        private final LivingEntityUser user;
        private final PaperSlot cursor;

        public ClickedSlotClickEvent(SokolPlugin plugin, InventoryClickEvent handle, PaperTreeNode node) {
            super(node, PaperSlot.slot(plugin, handle::getCurrentItem, handle::setCurrentItem));
            this.handle = handle;
            this.user = living(plugin, handle.getWhoClicked());
            cursor = PaperSlot.slot(plugin, handle.getView()::getCursor, handle.getView()::setCursor);
        }

        @Override public InventoryClickEvent handle() { return handle; }
        @Override public LivingEntityUser user() { return user; }
        @Override public PaperSlot cursor() { return cursor; }

        @Override public boolean cancelled() { return handle.isCancelled(); }
        @Override public void cancelled(boolean cancelled) { handle.setCancelled(cancelled); }

        @Override public boolean left() { return handle.isLeftClick(); }
        @Override public boolean right() { return handle.isRightClick(); }
        @Override public boolean shift() { return handle.isShiftClick(); }
    }

    final class CursorSlotClickEvent extends Base implements ItemTreeEvent.CursorSlotClickEvent, FromServer {
        private final InventoryClickEvent handle;
        private final LivingEntityUser user;
        private final PaperSlot clicked;

        public CursorSlotClickEvent(SokolPlugin plugin, InventoryClickEvent handle, PaperTreeNode node) {
            super(node, PaperSlot.slot(plugin, handle.getView()::getCursor, handle.getView()::setCursor));
            this.handle = handle;
            user = living(plugin, handle.getWhoClicked());
            clicked = PaperSlot.slot(plugin, handle::getCurrentItem, handle::setCurrentItem);
        }

        @Override public InventoryClickEvent handle() { return handle; }
        @Override public LivingEntityUser user() { return user; }
        @Override public PaperSlot clicked() { return clicked; }

        @Override public boolean cancelled() { return handle.isCancelled(); }
        @Override public void cancelled(boolean cancelled) { handle.setCancelled(cancelled); }

        @Override public boolean left() { return handle.isLeftClick(); }
        @Override public boolean right() { return handle.isRightClick(); }
        @Override public boolean shift() { return handle.isShiftClick(); }
    }

    final class InputEvent extends Base implements ItemTreeEvent.InputEvent {
        private final PlayerUser user;
        private final Inputs.Events.Input event;
        private boolean cancelled;

        public InputEvent(PaperTreeNode node, PlayerUser user, PaperSlot slot, Inputs.Events.Input event) {
            super(node, slot);
            this.user = user;
            this.event = event;
        }

        @Override public LivingEntityUser user() { return user; }
        @Override public InputType input() { return event.input(); }
        public Inputs.Events.Input event() { return event; }

        @Override public boolean cancelled() { return cancelled; }
        @Override public void cancelled(boolean cancelled) { this.cancelled = cancelled; }
    }

    final class RawInputEvent extends Base implements TreeEvent.ItemEvent {
        private final PlayerUser user;
        private final Inputs.Events.Input event;

        public RawInputEvent(PaperTreeNode node, PlayerUser user, PaperSlot slot, Inputs.Events.Input event) {
            super(node, slot);
            this.user = user;
            this.event = event;
        }

        @Override public PlayerUser user() { return user; }
        public Inputs.Events.Input event() { return event; }
    }

    final class Equip extends Base implements ItemTreeEvent.Equip {
        private final PlayerUser user;
        private final PaperSlot oldSlot;
        private boolean cancelled;

        public Equip(PaperTreeNode node, PaperSlot slot, PlayerUser user, PaperSlot oldSlot) {
            super(node, slot);
            this.user = user;
            this.oldSlot = oldSlot;
        }

        @Override public PlayerUser user() { return user; }
        @Override public PaperSlot oldSlot() { return oldSlot; }

        @Override public boolean cancelled() { return cancelled; }
        @Override public void cancelled(boolean cancelled) { this.cancelled = cancelled; }
    }

    final class Unequip extends Base implements ItemTreeEvent.Unequip {
        private final PlayerUser user;
        private final PaperSlot newSlot;
        private boolean cancelled;

        public Unequip(PaperTreeNode node, PaperSlot slot, PlayerUser user, PaperSlot newSlot) {
            super(node, slot);
            this.user = user;
            this.newSlot = newSlot;
        }

        @Override public PlayerUser user() { return user; }
        @Override public PaperSlot newSlot() { return newSlot; }

        @Override public boolean cancelled() { return cancelled; }
        @Override public void cancelled(boolean cancelled) { this.cancelled = cancelled; }
    }

    final class ShowItem extends Base implements ItemTreeEvent.ShowItem {
        private final EntityUser user;
        private boolean cancelled;

        public ShowItem(PaperTreeNode node, EntityUser user, PaperSlot slot) {
            super(node, slot);
            this.user = user;
        }

        @Override public EntityUser user() { return user; }

        @Override public boolean cancelled() { return cancelled; }
        @Override public void cancelled(boolean cancelled) { this.cancelled = cancelled; }
    }
}
