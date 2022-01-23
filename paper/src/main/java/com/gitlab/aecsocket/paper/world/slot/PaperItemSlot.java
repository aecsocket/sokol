package com.gitlab.aecsocket.paper.world.slot;

import com.github.aecsocket.sokol.core.world.EquipSlot;
import com.github.aecsocket.sokol.core.world.ItemSlot;
import com.gitlab.aecsocket.minecommons.core.CollectionBuilder;
import com.gitlab.aecsocket.paper.SokolPlugin;
import com.gitlab.aecsocket.paper.impl.PaperItemStack;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface PaperItemSlot extends ItemSlot<PaperItemStack> {
    SokolPlugin plugin();

    @Nullable ItemStack getRaw();

    void setRaw(@Nullable ItemStack item);

    @Override
    default Optional<PaperItemStack> get() {
        ItemStack item = getRaw();
        return item == null ? Optional.empty() : Optional.of(new PaperItemStack(plugin(), item));
    }

    @Override
    default void set(@Nullable PaperItemStack item) {
        setRaw(item == null ? null : item.handle());
    }

    interface Equip extends PaperItemSlot, EquipSlot<PaperItemStack> {
        BiMap<EquipmentSlot, Position> MAPPINGS = HashBiMap.create(CollectionBuilder.map(new HashMap<EquipmentSlot, Position>())
                .put(EquipmentSlot.HAND, Position.MAIN_HAND)
                .put(EquipmentSlot.OFF_HAND, Position.OFF_HAND)
                .put(EquipmentSlot.HEAD, Position.HEAD)
                .put(EquipmentSlot.CHEST, Position.CHEST)
                .put(EquipmentSlot.LEGS, Position.LEGS)
                .put(EquipmentSlot.FEET, Position.FEET)
                .build());

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
