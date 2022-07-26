package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.core.extension.join
import com.gitlab.aecsocket.alexandria.core.physics.Quaternion
import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.AlexandriaAPI
import com.gitlab.aecsocket.alexandria.paper.extension.*
import com.gitlab.aecsocket.sokol.core.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Chunk
import org.bukkit.World
import org.bukkit.block.BlockState
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import org.bukkit.inventory.meta.ItemMeta
import java.util.*
import kotlin.collections.ArrayList

private fun format(component: Component) = PlainTextComponentSerializer.plainText().serialize(component)

interface PaperNodeHost : NodeHost<PaperDataNode> {
    interface Physical : PaperNodeHost {
        val world: World
    }
}

interface PaperWorldHost : PaperNodeHost {
    override val locale get() = null
    val world: World
}

interface PaperChunkHost : PaperNodeHost.Physical {
    val chunk: Chunk

    override val locale get() = null
    override val world get() = chunk.world
}

interface PaperEntityHost : PaperNodeHost.Physical, EntityHost<PaperDataNode> {
    val entity: Entity

    override val locale: Locale? get() {
        val player = entity
        return if (player is Player) player.locale() else null
    }
    override val world get() = entity.world

    override var transform: Transform
        get() = entity.transform
        set(value) { entity.transform = value }

    override var looking: Quaternion
        get() = entity.looking
        set(value) { entity.looking = value }
}

interface PaperRenderHost : PaperNodeHost.Physical, NodeHost.Looking<PaperDataNode> {
    val render: NodeRender<*>

    override val locale get() = null
    override val world get() = render.world

    override var transform: Transform
        get() = render.transform
        set(value) { render.transform = value }

    override var looking: Quaternion
        get() = render.transform.rot
        set(value) { render.transform = render.transform.copy(rot = value) }
}

interface PaperBlockHost : PaperNodeHost.Physical, BlockHost<PaperDataNode> {
    val block: BlockState

    override val locale get() = null
    override val world get() = block.world

    override val transform: Transform
        get() = Transform(tl = block.location.vector())
}

interface PaperItemHost : PaperNodeHost, ItemHost<PaperDataNode> {
    override val locale get() = null
    override val holder: PaperItemHolder
    val stack: ItemStack

    override val key get() = stack.type.key
    override var amount: Int
        get() = stack.amount
        set(value) { stack.amount = value }

    override var name: Component?
        get() = readMeta { it.displayName() }
        set(value) {
            writeMeta { meta -> meta.displayName(
                value?.let { text().decoration(TextDecoration.ITALIC, false).append(it).build() }
            ) }
        }

    fun <R> readMeta(action: (ItemMeta) -> R): R

    fun writeMeta(action: (ItemMeta) -> Unit)
}

fun Sokol.useHostOf(
    holder: PaperItemHolder,
    stack: () -> ItemStack,
    action: (PaperItemHost) -> Unit,
    onDirty: (ItemMeta) -> Unit = { stack().itemMeta = it },
    onClean: () -> Unit = {},
): PaperItemHost {
    data class LoreSection(
        val header: Component?,
        val lines: Iterable<Component>,
    )

    val meta by lazy { stack().itemMeta }
    var dirty = false
    val lore = ArrayList<LoreSection>()
    val host = object : PaperItemHost {
        override val holder get() = holder
        override val stack get() = stack()

        override var node: PaperDataNode?
            get() = persistence.holderOf(stack())[persistence.kNode]?.let { persistence.nodeOf(it) }
            set(value) {
                writeMeta { meta ->
                    persistence.holderOf(meta.persistentDataContainer).apply {
                        value?.let {
                            set(persistence.kNode, persistence.newTag().apply { it.serialize(this) })
                        } ?: remove(persistence.kNode)
                    }
                }
            }

        override fun <R> readMeta(action: (ItemMeta) -> R): R {
            return action(meta)
        }

        override fun writeMeta(action: (ItemMeta) -> Unit) {
            action(meta)
            dirty = true
        }

        override fun addLore(lines: Iterable<Component>, header: Component?) {
            if (lines.any()) {
                lore.add(LoreSection(header, lines))
                dirty = true
            }
        }

        override fun toString() =
            "ItemHost(${format(stack().displayName())} x${stack().amount})"
    }
    action(host)
    if (dirty) {
        val headerWidth = lore.maxOfOrNull { sec ->
            sec.header?.let { AlexandriaAPI.widthOf(it) } ?: 0
        }

        meta.lore(lore
            .map { sec ->
                (sec.header?.let {
                    i18n.safe("item_lore_header") {
                        sub("header") { it }
                        raw("padding") {
                            headerWidth?.let { width ->
                                AlexandriaAPI.paddingOf(width - AlexandriaAPI.widthOf(it))
                            } ?: ""
                        }
                    }
                } ?: emptyList()) + sec.lines
            }
            .join({ it }, { itemLoreSeparator })
            .map { text().decoration(TextDecoration.ITALIC, false).append(it).build() }
        )
        onDirty(meta)
    } else {
        onClean()
    }
    return host
}

// todo this entire damn thing is a todo. i hate myself
fun Sokol.hostOf(world: World) = object : PaperWorldHost {
    override val world get() = world

    override var node: PaperDataNode?
        get() = TODO()
        set(value) {}
}

fun Sokol.hostOf(chunk: Chunk) = object : PaperChunkHost {
    override val chunk get() = chunk

    override var node: PaperDataNode?
        get() = TODO("Not yet implemented")
        set(value) {}
}

fun Sokol.hostOf(entity: Entity) = object : PaperEntityHost {
    override val entity get() = entity

    override var node: PaperDataNode?
        get() = TODO("Not yet implemented")
        set(value) {}

    override fun toString() =
        "EntityHost(${entity.name} @ ${entity.location.vector()})"
}

fun Sokol.hostOf(block: BlockState) = object : PaperBlockHost {
    override val block get() = block

    override var node: PaperDataNode?
        get() = TODO("Not yet implemented")
        set(value) {}

    override fun toString() =
        "BlockHost(${block.type.key} @ ${block.location.vector()})"
}

fun Sokol.hostOf(render: NodeRender<*>) = object : PaperRenderHost {
    override val render get() = render

    override var node: PaperDataNode?
        get() = render.root.node
        set(value) {
            if (value == null) render.remove()
            else render.root.node = value
        }

    override fun toString() =
        "RenderHost(${render.root.node.component.id} @ ${render.transform.tl})"
}

interface PaperItemHolder : ItemHolder<PaperDataNode> {
    override val host: PaperNodeHost?

    interface ByContainer : PaperItemHolder {
        val slotId: Int
    }

    interface ByEquipment : PaperItemHolder {
        val slot: EquipmentSlot
    }

    interface ByCursor : PaperItemHolder

    interface ByDropped : PaperItemHolder

    interface ByFrame : PaperItemHolder
}

private val EmptyHolder = object : PaperItemHolder {
    override val host get() = null

    override fun toString() =
        "EmptyHolder"
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

fun emptyHolder() = EmptyHolder

fun holderBy(host: PaperNodeHost) = object : PaperItemHolder {
    override val host get() = host
}

fun holderByContainer(host: PaperNodeHost, slotId: Int) = object : PaperItemHolder.ByContainer {
    override val host get() = host
    override val slotId get() = slotId
}

fun holderByPlayer(host: PaperNodeHost, player: Player, slot: Int): PaperItemHolder {
    return slot(player.inventory, slot)?.let { equipSlot -> object : PaperItemHolder.ByContainer, PaperItemHolder.ByEquipment {
        override val host get() = host
        override val slotId get() = slot
        override val slot get() = equipSlot
    } } ?: object : PaperItemHolder.ByContainer {
        override val host get() = host
        override val slotId get() = slot
    }
}

fun holderByEquipment(host: PaperNodeHost, slot: EquipmentSlot) = object : PaperItemHolder.ByEquipment {
    override val host get() = host
    override val slot get() = slot
}

fun holderByCursor(host: PaperNodeHost) = object : PaperItemHolder.ByCursor {
    override val host get() = host
}

fun holderByDropped(host: PaperNodeHost) = object : PaperItemHolder.ByDropped {
    override val host get() = host
}

fun holderByFrame(host: PaperNodeHost) = object : PaperItemHolder.ByFrame {
    override val host get() = host
}

/*
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

    interface OfStack : PaperNodeHost, ItemHost {
        val stack: ItemStack
        val holder: StackHolder

        override val key get() = stack.type.key
        override var amount: Int
            get() = stack.amount
            set(value) { stack.amount = value }

        fun readMeta(action: ItemMeta.() -> Unit)

        fun writeMeta(action: ItemMeta.() -> Unit)
    }

    data class OfWritableStack(
        override val holder: StackHolder,
        private val getStack: () -> ItemStack,
        private val getMeta: () -> ItemMeta,
        private val onDirty: () -> Unit
    ) : OfStack {
        override val stack get() = getStack()

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

 */
