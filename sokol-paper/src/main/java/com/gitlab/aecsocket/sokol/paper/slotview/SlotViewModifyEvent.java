package com.gitlab.aecsocket.sokol.paper.slotview;

import com.gitlab.aecsocket.sokol.paper.PaperTreeNode;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

public class SlotViewModifyEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final InventoryClickEvent handle;
    private final SlotViewPane.Item item;
    private final PaperTreeNode cursorNode;
    private boolean cancelled;

    public SlotViewModifyEvent(InventoryClickEvent handle, SlotViewPane.Item item, PaperTreeNode cursorNode) {
        this.handle = handle;
        this.item = item;
        this.cursorNode = cursorNode;
    }

    public InventoryClickEvent handle() { return handle; }
    public SlotViewPane.Item item() { return item; }
    public PaperTreeNode cursorNode() { return cursorNode; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    @Override public @NotNull HandlerList getHandlers() { return handlers; }
    public static @NotNull HandlerList getHandlerList() { return handlers; }
}
