package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.sokol.core.SokolComponent
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.Inventory

sealed interface ItemHolder : SokolComponent {
    override val componentType get() = ItemHolder::class.java

    interface Equipment : ItemHolder {
        val mob: LivingEntity
        val slot: EquipmentSlot
    }

    interface Container : ItemHolder {
        val inventory: Inventory
        val slotId: Int
    }
}

fun itemHolderOf(mob: LivingEntity, slot: EquipmentSlot) = object : ItemHolder.Equipment {
    override val mob get() = mob
    override val slot get() = slot
}

fun itemHolderOf(inventory: Inventory, slotId: Int) = object : ItemHolder.Container {
    override val inventory get() = inventory
    override val slotId get() = slotId
}

private val SLOT_MAPPING = mapOf(
    40 to EquipmentSlot.OFF_HAND,
    36 to EquipmentSlot.FEET,
    37 to EquipmentSlot.LEGS,
    38 to EquipmentSlot.CHEST,
    39 to EquipmentSlot.HEAD,
)

fun itemHolderOfPlayer(player: Player, slotId: Int): ItemHolder {
    return SLOT_MAPPING[slotId]?.let { slot ->
        object : ItemHolder.Equipment, ItemHolder.Container {
            override val mob get() = player
            override val slot get() = slot
            override val inventory get() = player.inventory
            override val slotId get() = slotId
        }
    } ?: object : ItemHolder.Container {
        override val inventory get() = player.inventory
        override val slotId get() = slotId
    }
}
