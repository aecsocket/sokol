package com.gitlab.aecsocket.sokol.paper.wrapper.slot;

import com.gitlab.aecsocket.minecommons.core.CollectionBuilder;
import com.gitlab.aecsocket.sokol.core.wrapper.UserSlot;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import com.gitlab.aecsocket.sokol.paper.impl.PaperNode;
import com.gitlab.aecsocket.sokol.paper.wrapper.PaperItem;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.LivingUser;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;

public interface EquippedItemSlot extends PaperItemSlot, UserSlot<PaperItem> {
    Map<EquipmentSlot, Position> POSITION_MAP = CollectionBuilder.map(new HashMap<EquipmentSlot, Position>())
            .put(EquipmentSlot.HAND, Position.HAND)
            .put(EquipmentSlot.OFF_HAND, Position.OFF_HAND)
            .put(EquipmentSlot.HEAD, Position.HEAD)
            .put(EquipmentSlot.CHEST, Position.CHEST)
            .put(EquipmentSlot.LEGS, Position.LEGS)
            .put(EquipmentSlot.FEET, Position.FEET)
            .build();

    @Override LivingUser user();
    EquipmentSlot slot();

    default LivingEntity entity() { return user().entity(); }
    @Override default Position position() { return POSITION_MAP.get(slot()); }

    @Override
    default @Nullable ItemStack bukkitGet() {
        //noinspection ConstantConditions - equipment is never null
        return entity().getEquipment().getItem(slot());
    }

    @Override
    default void bukkitSet(@Nullable ItemStack val) {
        //noinspection ConstantConditions - equipment is never null
        entity().getEquipment().setItem(slot(), val);
    }

    static EquippedItemSlot slot(SokolPlugin plugin, LivingUser user, EquipmentSlot slot) {
        return new EquippedItemSlotImpl(plugin, user, slot);
    }
}
