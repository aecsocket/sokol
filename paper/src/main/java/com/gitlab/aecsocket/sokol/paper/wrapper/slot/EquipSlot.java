package com.gitlab.aecsocket.sokol.paper.wrapper.slot;

import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public interface EquipSlot extends PaperSlot {
    LivingEntity entity();
    EquipmentSlot slot();

    @Override default ItemStack paperGet() {
        //noinspection ConstantConditions - equipment is never going to be null. Bukkit is stupid.
        return entity().getEquipment().getItem(slot());
    }
    @Override default void paperSet(ItemStack item) {
        //noinspection ConstantConditions - equipment is never going to be null. Bukkit is stupid.
        entity().getEquipment().setItem(slot(), item);
    }
}
