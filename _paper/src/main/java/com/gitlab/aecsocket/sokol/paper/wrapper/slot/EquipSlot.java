package com.gitlab.aecsocket.sokol.paper.wrapper.slot;

import com.gitlab.aecsocket.sokol.core.wrapper.UserSlot;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.LivingEntityUser;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

public interface EquipSlot extends PaperSlot, UserSlot {
    Set<EquipmentSlot> HAND_SLOTS = Set.of(EquipmentSlot.HAND, EquipmentSlot.OFF_HAND);
    Set<EquipmentSlot> ARMOR_SLOTS = Set.of(EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD);

    @Override LivingEntityUser user();
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

    @Override default boolean inHand() { return HAND_SLOTS.contains(slot()); }
    @Override default boolean inArmor() { return ARMOR_SLOTS.contains(slot()); }
}
