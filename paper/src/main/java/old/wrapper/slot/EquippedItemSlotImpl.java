package old.wrapper.slot;

import old.SokolPlugin;
import old.wrapper.user.LivingUser;
import org.bukkit.inventory.EquipmentSlot;

/* package */ record EquippedItemSlotImpl(
        SokolPlugin plugin,
        LivingUser user,
        EquipmentSlot slot
) implements EquippedItemSlot {}
