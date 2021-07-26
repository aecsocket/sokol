package com.gitlab.aecsocket.sokol.paper.wrapper.slot;

import com.gitlab.aecsocket.minecommons.paper.plugin.ProtocolConstants;
import com.gitlab.aecsocket.sokol.core.wrapper.UserSlot;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.PlayerUser;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.PlayerInventory;

public interface PlayerInventorySlot extends InventorySlot, UserSlot {
    @Override PlayerUser user();
    @Override PlayerInventory inventory();
    int heldSlot();

    @Override
    default boolean inHand() {
        if (slot() == heldSlot())
            return true;
        EquipmentSlot slot = ProtocolConstants.SLOT_IDS.inverse().get(slot());
        if (slot == null)
            return false;
        return EquipSlot.HAND_SLOTS.contains(slot);
    }

    @Override
    default boolean inArmor() {
        EquipmentSlot slot = ProtocolConstants.SLOT_IDS.inverse().get(slot());
        if (slot == null)
            return false;
        return EquipSlot.ARMOR_SLOTS.contains(slot);
    }
}
