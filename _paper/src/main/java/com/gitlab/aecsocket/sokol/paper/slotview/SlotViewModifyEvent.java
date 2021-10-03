package com.gitlab.aecsocket.sokol.paper.slotview;

import com.gitlab.aecsocket.sokol.paper.PaperTreeNode;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class SlotViewModifyEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final InventoryClickEvent handle;
    private final SlotViewPane.Item item;
    private final @Nullable PaperTreeNode cursorNode;
    private boolean cancelled;

    public SlotViewModifyEvent(InventoryClickEvent handle, SlotViewPane.Item item, @Nullable PaperTreeNode cursorNode) {
        this.handle = handle;
        this.item = item;
        this.cursorNode = cursorNode;
    }

    public InventoryClickEvent handle() { return handle; }
    public SlotViewPane.Item item() { return item; }
    public Optional<PaperTreeNode> cursorNode() { return Optional.ofNullable(cursorNode); }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    @Override public @NotNull HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
