package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.PlayerInventorySlot
import com.gitlab.aecsocket.sokol.core.SokolComponent
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.Inventory

sealed interface ItemHolder : SokolComponent {
    override val componentType get() = ItemHolder::class

    interface ByMob : ItemHolder {
        val mob: Entity
    }

    interface ByBlock : ItemHolder {
        val block: Block
    }

    interface ByContainerItem : ItemHolder {
        val parent: ItemHolder
        val slotId: Int
    }

    interface InCursor : ByMob {
        override val mob: Player
    }

    interface InEquipment : ByMob {
        override val mob: LivingEntity
        val slot: EquipmentSlot
    }

    interface InContainer : ItemHolder {
        val inventory: Inventory
        val slotId: Int
    }

    companion object {
        fun inCursor(mob: Player) = object : InCursor {
            override val mob get() = mob

            override fun toString() =
                "ItemHolder.inCursor($mob)"
        }

        fun inEquipment(mob: LivingEntity, slot: EquipmentSlot) = object : InEquipment {
            override val mob get() = mob
            override val slot get() = slot

            override fun toString() =
                "ItemHolder.inEquipment(mob=$mob, slot=$slot)"
        }

        fun inContainer(inventory: Inventory, slotId: Int) = object : InContainer {
            override val inventory get() = inventory
            override val slotId get() = slotId

            override fun toString() =
                "ItemHolder.inContainer(inventory=$inventory, slotId=$slotId)"
        }

        fun byMob(mob: Entity) = object : ByMob {
            override val mob get() = mob

            override fun toString() =
                "ItemHolder.byMob($mob)"
        }

        fun byPlayer(mob: Player, slotId: Int): ByMob {
            return PlayerInventorySlot.intToSlot(mob.inventory, slotId)?.let { slot ->
                object : ByMob, InEquipment, InContainer {
                    override val mob get() = mob
                    override val slot get() = slot
                    override val inventory get() = mob.inventory
                    override val slotId get() = slotId

                    override fun toString() =
                        "ItemHolder.byPlayer/InEquipment(mob=$mob, slot=$slot, slotId=$slotId)"
                }
            } ?: object : ByMob, InContainer {
                override val mob get() = mob
                override val inventory get() = mob.inventory
                override val slotId get() = slotId

                override fun toString() =
                    "ItemHolder.byPlayer/InContainer(mob=$mob, slotId=$slotId)"
            }
        }

        fun byBlock(block: Block, inventory: Inventory, slotId: Int): ByBlock = object : ByBlock, InContainer {
            override val block get() = block
            override val inventory get() = inventory
            override val slotId get() = slotId

            override fun toString() =
                "ItemHolder.byBlock(block=$block, inventory=$inventory, slotId=$slotId)"
        }

        fun byContainerItem(parent: ItemHolder, slotId: Int) = object : ByContainerItem {
            override val parent get() = parent
            override val slotId get() = slotId

            override fun toString() =
                "ItemHolder.byContainerItem(parent=$parent, slotId=$slotId)"
        }
    }
}
