package com.gitlab.aecsocket.sokol.paper.wrapper.slot;

import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.LivingUser;
import org.bukkit.inventory.EquipmentSlot;

/* package */ record EquippedItemSlotImpl(
        SokolPlugin plugin,
        LivingUser user,
        EquipmentSlot slot
) implements EquippedItemSlot {}
