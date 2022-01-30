package com.github.aecsocket.sokol.paper.world.slot;

import com.github.aecsocket.sokol.core.world.EquipSlot;
import com.github.aecsocket.sokol.paper.SokolPlugin;

import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

/* package */ record PaperEquipSlotImpl(
    SokolPlugin plugin,
    LivingEntity entity,
    EquipSlot.Position position,
    EquipmentSlot slot
) implements PaperItemSlot.Equip {
    @Override
    public @Nullable ItemStack raw() {
        //noinspection ConstantConditions
        return entity.getEquipment().getItem(slot);
    }

    @Override
    public void raw(@Nullable ItemStack item) {
        //noinspection ConstantConditions
        entity.getEquipment().setItem(slot, item);
    }
}
