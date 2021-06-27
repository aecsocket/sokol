package com.gitlab.aecsocket.sokol.paper.wrapper.slot;

import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface InventorySlot extends PaperSlot {
    @NotNull Inventory inventory();
    int slot();

    @Override default ItemStack paperGet() { return inventory().getItem(slot()); }
    @Override default void paperSet(ItemStack item) { inventory().setItem(slot(), item); }

    static InventorySlot of(SokolPlugin plugin, Inventory inventory, int slot) {
        return new InventorySlot() {
            @Override public SokolPlugin plugin() { return plugin; }
            @Override public @NotNull Inventory inventory() { return inventory; }
            @Override public int slot() { return slot; }
        };
    }
}
