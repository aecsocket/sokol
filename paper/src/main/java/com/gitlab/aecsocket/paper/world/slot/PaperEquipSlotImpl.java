package com.gitlab.aecsocket.paper.world.slot;

import com.github.aecsocket.sokol.core.world.EquipSlot;
import com.gitlab.aecsocket.paper.SokolPlugin;

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
    public ItemStack getRaw() {
        //noinspection ConstantConditions - no it's not.
        return entity.getEquipment().getItem(slot);
    }

    @Override
    public void setRaw(@Nullable ItemStack item) {
        //noinspection ConstantConditions - no it's not.
        entity.getEquipment().setItem(slot, item);
    }
}
