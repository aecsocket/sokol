package com.gitlab.aecsocket.sokol.paper.wrapper.slot;

import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public interface InventorySlot extends PaperSlot {
    Inventory inventory();
    int slot();

    @Override default ItemStack paperGet() { return inventory().getItem(slot()); }
    @Override default void paperSet(ItemStack item) { inventory().setItem(slot(), item); }
}
