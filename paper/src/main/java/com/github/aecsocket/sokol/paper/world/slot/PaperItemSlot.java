package com.github.aecsocket.sokol.paper.world.slot;

import java.util.HashMap;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.github.aecsocket.minecommons.core.CollectionBuilder;
import com.github.aecsocket.sokol.core.world.EquipSlot;
import com.github.aecsocket.sokol.core.world.ItemSlot;
import com.github.aecsocket.sokol.paper.PaperItemStack;
import com.github.aecsocket.sokol.paper.SokolPlugin;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;

import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface PaperItemSlot extends ItemSlot<PaperItemStack> {
    SokolPlugin plugin();

    @Nullable ItemStack raw();
    void raw(@Nullable ItemStack item);

    @Override
    default Optional<PaperItemStack> get() {
        ItemStack item = raw();
        return item == null ? Optional.empty() : Optional.of(new PaperItemStack(plugin(), item));
    }

    @Override
    default void set(@Nullable PaperItemStack item) {
        raw(item == null ? null : item.handle());
    }

    interface Equip extends PaperItemSlot, EquipSlot<PaperItemStack> {
        BiMap<EquipmentSlot, Position> MAPPINGS = ImmutableBiMap.<EquipmentSlot, Position>builder()
                .put(EquipmentSlot.HAND, Position.MAIN_HAND)
                .put(EquipmentSlot.OFF_HAND, Position.OFF_HAND)
                .put(EquipmentSlot.HEAD, Position.HEAD)
                .put(EquipmentSlot.CHEST, Position.CHEST)
                .put(EquipmentSlot.LEGS, Position.LEGS)
                .put(EquipmentSlot.FEET, Position.FEET)
                .build();

        LivingEntity entity();
        EquipmentSlot slot();
    }

    static PaperItemSlot itemSlot(SokolPlugin plugin, Supplier<@Nullable ItemStack> get, Consumer<@Nullable ItemStack> set) {
        return new PaperItemSlotImpl(plugin, get, set);
    }

    static Equip itemSlot(SokolPlugin plugin, LivingEntity entity, EquipmentSlot slot) {
        return new PaperEquipSlotImpl(plugin, entity, Equip.MAPPINGS.get(slot), slot);
    }
}
