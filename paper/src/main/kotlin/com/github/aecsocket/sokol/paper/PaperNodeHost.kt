package com.github.aecsocket.sokol.paper

import com.github.aecsocket.alexandria.core.physics.Quaternion
import com.github.aecsocket.alexandria.core.physics.Transform
import com.github.aecsocket.alexandria.paper.extension.*
import com.github.aecsocket.sokol.core.NodeHost
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.translatable
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Chunk
import org.bukkit.World
import org.bukkit.block.BlockState
import org.bukkit.entity.Entity
import org.bukkit.entity.Item
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import org.bukkit.inventory.meta.ItemMeta

private fun format(component: Component) = PlainTextComponentSerializer.plainText().serialize(component)

interface PaperNodeHost : NodeHost {
    interface Static : PaperNodeHost {
        val world: World
        val transform: Transform
    }

    interface Dynamic : Static {
        override var transform: Transform
    }

    interface Looking : Dynamic {
        var looking: Quaternion
    }

    data class OfWorld(val world: World) : PaperNodeHost {
        override fun toString() = "World[${world.name}]"
    }

    data class OfChunk(val chunk: Chunk) : PaperNodeHost {
        override fun toString() = "Chunk[@ ${chunk.world.name}(${chunk.x}, ${chunk.z})]"
    }

    data class OfEntity(val entity: Entity) : Looking {
        override val world get() = entity.world
        override var transform
            get() = entity.transform
            set(value) { entity.transform = value }
        override var looking: Quaternion
            get() = entity.looking
            set(value) { entity.looking = value }

        override fun toString(): String {
            val (x, y, z) = entity.location
            return "Entity[${format(entity.name())} of ${entity.type} @ ${world.name}($x, $y, $z)]"
        }
    }

    interface OfStack : PaperNodeHost {
        val stack: ItemStack
        val holder: StackHolder

        fun readMeta(action: ItemMeta.() -> Unit)

        fun writeMeta(action: ItemMeta.() -> Unit)
    }

    data class OfWritableStack(
        override val holder: StackHolder,
        private val getStack: () -> ItemStack,
        private val getMeta: () -> ItemMeta,
        private val onDirty: () -> Unit
    ) : OfStack {
        override val stack: ItemStack
            get() = getStack()

        override fun readMeta(action: ItemMeta.() -> Unit) {
            action(getMeta())
        }

        override fun writeMeta(action: ItemMeta.() -> Unit) {
            action(getMeta())
            onDirty()
        }

        override fun toString() =
            "WritableStack[${format(getMeta().displayName() ?: translatable(stack.translationKey()))} x${stack.itemMeta} by $holder]"
    }

    data class OfBlock(val block: BlockState) : Static {
        override val world get() = block.world
        override val transform
            get() = Transform(tl = block.location.vector())

        override fun toString() = "Block[${block.type} @ ${block.world.name}(${block.x}, ${block.y}, ${block.z})]"
    }
}

interface StackHolder {
    val parent: PaperNodeHost

    interface ByInventory : StackHolder {
        val inventory: Inventory
        val slotId: Int
    }

    interface ByEntity : StackHolder {
        override val parent: PaperNodeHost.OfEntity
    }

    interface ByBlock : StackHolder {
        override val parent: PaperNodeHost.OfBlock
        val block: BlockState
    }

    interface ByEquipment : ByEntity {
        val entity: LivingEntity
        val slot: EquipmentSlot
    }

    interface ByItemEntity : ByEntity {
        val entity: Item
    }

    interface ByItemFrame : ByEntity {
        val entity: ItemFrame
    }

    interface ByCursor : ByEntity {
        val player: Player
    }

    interface ByShulkerBox : StackHolder {
        val slotId: Int
    }

    companion object {
        fun byEquipment(
            host: PaperNodeHost.OfEntity,
            entity: LivingEntity,
            slot: EquipmentSlot
        ) = object : ByEquipment {
            override val parent: PaperNodeHost.OfEntity
                get() = host
            override val entity: LivingEntity
                get() = entity
            override val slot: EquipmentSlot
                get() = slot
        }

        fun byItemEntity(host: PaperNodeHost.OfEntity, entity: Item) = object : ByItemEntity {
            override val parent: PaperNodeHost.OfEntity
                get() = host
            override val entity: Item
                get() = entity
        }

        fun byItemFrame(host: PaperNodeHost.OfEntity, entity: ItemFrame) = object : ByItemFrame {
            override val parent: PaperNodeHost.OfEntity
                get() = host
            override val entity: ItemFrame
                get() = entity
        }

        fun byBlock(host: PaperNodeHost.OfBlock) = object : ByBlock {
            override val parent: PaperNodeHost.OfBlock
                get() = host
            override val block: BlockState
                get() = host.block
        }

        fun byCursor(host: PaperNodeHost.OfEntity, player: Player) = object : ByCursor {
            override val parent: PaperNodeHost.OfEntity
                get() = host
            override val player: Player
                get() = player
        }

        fun byShulkerBox(host: PaperNodeHost, slotId: Int) = object : ByShulkerBox {
            override val parent: PaperNodeHost
                get() = host
            override val slotId: Int
                get() = slotId
        }

        fun byPlayer(
            host: PaperNodeHost.OfEntity,
            entity: Player,
            slotId: Int
        ): ByEntity {
            val inventory = entity.inventory
            return slot(inventory, slotId)?.let { slot ->
                object : ByInventory, ByEquipment {
                    override val parent: PaperNodeHost.OfEntity
                        get() = host
                    override val entity: LivingEntity
                        get() = entity
                    override val inventory: Inventory
                        get() = inventory
                    override val slotId: Int
                        get() = slotId
                    override val slot: EquipmentSlot
                        get() = slot
                }
            } ?: object : ByEntity, ByInventory {
                override val parent: PaperNodeHost.OfEntity
                    get() = host
                override val inventory: Inventory
                    get() = inventory
                override val slotId: Int
                    get() = slotId
            }
        }

        private val SLOTS = mapOf(
            40 to EquipmentSlot.OFF_HAND,
            36 to EquipmentSlot.FEET,
            37 to EquipmentSlot.LEGS,
            38 to EquipmentSlot.CHEST,
            39 to EquipmentSlot.HEAD
        )

        private fun slot(inventory: PlayerInventory, slotId: Int) =
            SLOTS[slotId] ?: if (slotId == inventory.heldItemSlot) EquipmentSlot.HAND else null
    }
}

fun StackHolder.root(): PaperNodeHost {
    val parent = this.parent
    return if (parent is PaperNodeHost.OfStack) {
        parent.holder.root()
    } else parent
}
