package com.gitlab.aecsocket.sokol.paper;

import com.gitlab.aecsocket.minecommons.core.InputType;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Vector3;
import com.gitlab.aecsocket.minecommons.paper.PaperUtils;
import com.gitlab.aecsocket.minecommons.paper.inputs.Inputs;
import com.gitlab.aecsocket.sokol.core.tree.event.ItemTreeEvent;
import com.gitlab.aecsocket.sokol.core.tree.event.TreeEvent;
import com.gitlab.aecsocket.sokol.paper.wrapper.slot.PaperSlot;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.EntityUser;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.LivingEntityUser;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.PaperUser;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.PlayerUser;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.gitlab.aecsocket.sokol.paper.wrapper.user.PaperUser.*;

public interface PaperEvent extends TreeEvent.ItemEvent {
    @Override PaperTreeNode node();
    @Override PaperUser user();
    @Override PaperSlot slot();

    interface FromServer extends PaperEvent {
        Event handle();
    }

    abstract class Base extends TreeEvent.BaseItemEvent implements PaperEvent {
        protected final PaperTreeNode node;
        protected final PaperSlot slot;

        public Base(PaperTreeNode node, PaperSlot slot) {
            this.node = node;
            this.slot = slot;
        }

        @Override public PaperTreeNode node() { return node; }
        @Override public PaperSlot slot() { return slot; }
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

    final class ClickedSlotClick extends Base implements ItemTreeEvent.ClickedSlotClick, FromServer {
        private final InventoryClickEvent handle;
        private final LivingEntityUser user;
        private final PaperSlot cursor;

        public ClickedSlotClick(SokolPlugin plugin, InventoryClickEvent handle, PaperTreeNode node) {
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

    final class CursorSlotClick extends Base implements ItemTreeEvent.CursorSlotClick, FromServer {
        private final InventoryClickEvent handle;
        private final LivingEntityUser user;
        private final PaperSlot clicked;

        public CursorSlotClick(SokolPlugin plugin, InventoryClickEvent handle, PaperTreeNode node) {
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

    final class Input extends Base implements ItemTreeEvent.Input {
        private final PlayerUser user;
        private final Inputs.Events.Input event;
        private boolean cancelled;

        public Input(PaperTreeNode node, PlayerUser user, PaperSlot slot, Inputs.Events.Input event) {
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
        private final @Nullable PaperSlot oldSlot;
        private boolean cancelled;

        public Equip(PaperTreeNode node, PaperSlot slot, PlayerUser user, @Nullable PaperSlot oldSlot) {
            super(node, slot);
            this.user = user;
            this.oldSlot = oldSlot;
        }

        @Override public PlayerUser user() { return user; }
        @Override public @Nullable PaperSlot oldSlot() { return oldSlot; }

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

    final class BlockBreak extends Base implements ItemTreeEvent.BlockBreak, FromServer {
        private final BlockBreakEvent handle;
        private final PlayerUser user;
        private final Vector3 position;

        public BlockBreak(PaperTreeNode node, PaperSlot slot, BlockBreakEvent handle, PlayerUser user) {
            super(node, slot);
            this.handle = handle;
            this.user = user;
            position = PaperUtils.toCommons(handle.getBlock().getLocation());
        }

        @Override public BlockBreakEvent handle() { return handle; }
        @Override public PlayerUser user() { return user; }
        @Override public Vector3 position() { return position; }

        @Override public boolean cancelled() { return handle.isCancelled(); }
        @Override public void cancelled(boolean cancelled) { handle.setCancelled(cancelled); }
    }

    final class BlockPlace extends Base implements ItemTreeEvent.BlockPlace, FromServer {
        private final BlockPlaceEvent handle;
        private final PlayerUser user;
        private final Vector3 position;

        public BlockPlace(PaperTreeNode node, PaperSlot slot, BlockPlaceEvent handle, PlayerUser user) {
            super(node, slot);
            this.handle = handle;
            this.user = user;
            position = PaperUtils.toCommons(handle.getBlock().getLocation());
        }

        @Override public BlockPlaceEvent handle() { return handle; }
        @Override public PlayerUser user() { return user; }
        @Override public Vector3 position() { return position; }

        @Override public boolean cancelled() { return handle.isCancelled(); }
        @Override public void cancelled(boolean cancelled) { handle.setCancelled(cancelled); }
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
