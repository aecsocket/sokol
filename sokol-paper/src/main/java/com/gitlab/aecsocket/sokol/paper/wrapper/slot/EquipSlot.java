package com.gitlab.aecsocket.sokol.paper.wrapper.slot;

import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface EquipSlot extends PaperSlot {
    @NotNull LivingEntity entity();
    @NotNull EquipmentSlot slot();

    @Override default ItemStack paperGet() { return entity().getEquipment().getItem(slot()); }
    @Override default void paperSet(ItemStack item) { entity().getEquipment().setItem(slot(), item); }

    static EquipSlot of(SokolPlugin plugin, LivingEntity entity, EquipmentSlot slot) {
        return new EquipSlot() {
            @Override public SokolPlugin plugin() { return plugin; }
            @Override public @NotNull LivingEntity entity() { return entity; }
            @Override public @NotNull EquipmentSlot slot() { return slot; }
            @Override public String toString() { return slot + " @ " + entity; }
        };
    }
}
