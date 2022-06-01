package com.github.aecsocket.sokol.paper

import com.github.aecsocket.alexandria.paper.extension.component1
import com.github.aecsocket.alexandria.paper.extension.component2
import com.github.aecsocket.alexandria.paper.extension.component3
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.Container
import org.bukkit.block.TileState
import org.bukkit.entity.Entity
import org.bukkit.entity.Item
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.PlayerInventory
import java.util.*

enum class HostType {
    WORLD,
    CHUNK,
    ENTITY,
    STACK,
    BLOCK
}

private const val TAG = "tag"
private const val BUKKIT_PDC = "PublicBukkitValues"
private const val BLOCK_ENTITY_TAG = "BlockEntityTag"

interface HostsResolved {
    val possible: Int
    val marked: Int
}

/*
private fun plainText(component: Component) = PlainTextComponentSerializer.plainText().serialize(component)

private fun format(entity: Entity) = plainText(entity.name())

private fun format(block: Block): String {
    val (x, y, z) = block.location
    return "${block.world.name}@($x, $y, $z)"
}

sealed interface ServerElement {
    data class OfWorld(val world: World): ServerElement {
        override fun toString() = "World[${world.name}]"
    }

    data class OfChunk(val chunk: Chunk): ServerElement {
        override fun toString() = "Chunk[${chunk.world.name}@(${chunk.x}, ${chunk.z})]"
    }

    data class OfEntity(val entity: Entity) : ServerElement {
        override fun toString(): String {
            val (x, y, z) = entity.location
            return "Entity[${format(entity)} ${entity.world.name}@($x, $y, $z)]"
        }
    }

    data class OfStack(
        private val nms: ItemStack,
        val holder: StackHolder
    ) : ServerElement {
        val stack by lazy { nms.bukkitStack }
        val meta by lazy { stack.itemMeta }

        override fun toString(): String {
            val name = plainText(meta?.displayName() ?: translatable(stack.translationKey()))
            return "Stack[$name x${stack.amount}] by $holder"
        }
    }

    data class OfBlock(val block: Block, val state: TileState) : ServerElement {
        override fun toString() = format(block)
    }
}

sealed interface StackHolder {
    sealed interface ByEntity : StackHolder {
        val entity: Entity
    }

    sealed interface ByEquipment : ByEntity {
        override val entity: org.bukkit.entity.LivingEntity
        val equipmentSlot: org.bukkit.inventory.EquipmentSlot
    }

    sealed interface ByInventory : StackHolder {
        val inventory: Inventory
        val slotNumber: Int
    }

    data class ByEntityEquipment(
        override val entity: org.bukkit.entity.LivingEntity,
        override val equipmentSlot: org.bukkit.inventory.EquipmentSlot
    ) : ByEquipment {
        override fun toString() = "ByEntityEquipment[${format(entity)} : $equipmentSlot]"
    }

    data class ByPlayerEquipment(
        override val entity: org.bukkit.entity.Player,
        override val equipmentSlot: org.bukkit.inventory.EquipmentSlot
    ) : ByEquipment, ByInventory {
        override val inventory: PlayerInventory
            get() = entity.inventory
        override val slotNumber: Int
            get() = SLOTS_INT[equipmentSlot] ?: inventory.heldItemSlot

        override fun toString() = "ByPlayerEquipment[${format(entity)} : $equipmentSlot -> $slotNumber]"
    }

    data class ByPlayerInventory(
        val entity: org.bukkit.entity.Player,
        override val slotNumber: Int
    ) : ByInventory {
        override val inventory: PlayerInventory
            get() = entity.inventory

        override fun toString() = "ByPlayerInventory[${format(entity)} : $slotNumber]"
    }

    data class ByItemEntity(override val entity: Item) : ByEntity

    data class ByItemFrame(override val entity: org.bukkit.entity.ItemFrame): ByEntity

    data class ByBlock(
        val block: Block,
        override val slotNumber: Int
    ) : ByInventory {
        val state by lazy { block.state as Container }
        override val inventory: Inventory
            get() = state.inventory

        override fun toString() = "ByBlock[${format(block)} : $slotNumber]"
    }

    sealed interface ByStack : StackHolder {
        val parent: ServerElement.OfStack
    }

    data class ByShulkerBox(
        override val parent: ServerElement.OfStack,
        val slotNumber: Int
    ) : ByStack {
        override fun toString() = "ByShulkerBox[: $slotNumber] by $parent"
    }

    data class ByCursor(
        override val entity: org.bukkit.entity.Player
    ) : ByEntity {
        val view = entity.openInventory

        override fun toString() = "ByCursor[${format(entity)}]"
    }

    companion object {
        private val SLOTS_INT = mapOf(
            org.bukkit.inventory.EquipmentSlot.OFF_HAND to 40,
            org.bukkit.inventory.EquipmentSlot.FEET to 36,
            org.bukkit.inventory.EquipmentSlot.LEGS to 37,
            org.bukkit.inventory.EquipmentSlot.CHEST to 38,
            org.bukkit.inventory.EquipmentSlot.HEAD to 39
        )
    }
}

internal class HostResolver(
    val marker: NamespacedKey,
    val containerItems: Boolean,
    val containerBlocks: Boolean,
    private val callback: (ServerElement) -> Unit
) {
    private val markerStr = marker.toString()

    private data class HostsResolvedImpl(
        override var possible: Int = 0,
        override var marked: Int = 0
    ) : HostsResolved

    fun resolve(): Map<HostType, HostsResolved> = Operation().let {
        it.resolve()
        it.resolved
    }

    private inner class Operation {
        val resolved = EnumMap<HostType, HostsResolvedImpl>(HostType::class.java).apply {
            HostType.values().forEach { put(it, HostsResolvedImpl()) }
        }

        fun applyWorld(world: World) {
            val resolved = resolved[HostType.WORLD]!!
            resolved.possible++
            if (world.persistentDataContainer.has(marker)) {
                callback(ServerElement.OfWorld(world))
                resolved.marked++
            }
        }

        fun applyChunk(chunk: LevelChunk) {
            val resolved = resolved[HostType.CHUNK]!!
            resolved.possible++
            if (chunk.persistentDataContainer.has(marker)) {
                callback(ServerElement.OfChunk(chunk.bukkitChunk))
                resolved.marked++
            }
        }

        fun applyEntity(entity: Entity) {
            val resolved = resolved[HostType.ENTITY]!!
            resolved.possible++

            if (entity.persistentDataContainer.has(marker)) {
                callback(ServerElement.OfEntity(entity))
                resolved.marked++
            }

            when (val nms = (entity as CraftEntity).handle) {
                is Player -> {
                    val player = entity as org.bukkit.entity.Player
                    EquipmentSlot.values().forEach { slot ->
                        applyStack(nms.getItemBySlot(slot), StackHolder.ByPlayerEquipment(
                            player, SLOTS_NMS[slot]!!
                        ))
                    }
                    val selected = nms.inventory.selected
                    nms.inventory.items.forEachIndexed { idx, stack ->
                        if (idx != selected) {
                            applyStack(stack, StackHolder.ByPlayerInventory(
                                player, idx
                            ))
                        }
                    }
                }
                is LivingEntity -> {
                    entity as org.bukkit.entity.LivingEntity
                    EquipmentSlot.values().forEach { slot ->
                        applyStack(nms.getItemBySlot(slot), StackHolder.ByEntityEquipment(
                            entity, SLOTS_NMS[slot]!!
                        ))
                    }
                }
                is ItemFrame -> applyStack(nms.item, StackHolder.ByItemFrame(entity as org.bukkit.entity.ItemFrame))
                is ItemEntity -> applyStack(nms.item, StackHolder.ByItemEntity(entity as Item))
            }
        }

        fun marked(tag: CompoundTag) =
            tag.contains(BUKKIT_PDC) && (tag.get(BUKKIT_PDC) as CompoundTag).contains(markerStr)

        fun applyStack(stack: ItemStack, holder: StackHolder) {
            if (stack.isEmpty)
                return
            val resolved = resolved[HostType.STACK]!!
            resolved.possible++

            // TODO:
            val element = ServerElement.OfStack(stack, holder)
            stack.tag?.let { tag ->
                if (marked(tag)) {
                    callback(element)
                    resolved.marked++
                }

                if (!containerItems)
                    return

                // nms.world.item.BlockItem - onDestroyed
                if (tag.contains(BLOCK_ENTITY_TAG)) {
                    val blockTag = tag.getCompound(BLOCK_ENTITY_TAG)
                    if (blockTag.contains("Items", 9)) {
                        blockTag.getList("Items", 10).forEach { itemTag ->
                            resolved.possible++
                            // check if the item is marked too, before applying it
                            // also, because shulkers can't contain other shulkers,
                            // this shouldn't miss any items
                            itemTag as CompoundTag
                            if (itemTag.contains(TAG) && marked(itemTag.get(TAG) as CompoundTag)) {
                                callback(
                                    ServerElement.OfStack(
                                        // todo no
                                        ItemStack.of(itemTag), StackHolder.ByShulkerBox(
                                            element, itemTag.getByte("Slot").toInt()
                                        )
                                    )
                                )
                                resolved.marked++
                            }
                        }
                    }
                }
            }
        }

        fun applyBlock(world: World, block: BlockEntity) {
            val resolved = resolved[HostType.BLOCK]!!
            resolved.possible++

            val pos = block.blockPos
            val bukkit = world.getBlockAt(pos.x, pos.y, pos.z)
            val state by lazy { bukkit.getState(false) as TileState }
            if (block.persistentDataContainer.has(marker)) {
                callback(ServerElement.OfBlock(bukkit, state))
                resolved.marked++
            }

            if (!containerBlocks)
                return

            if (block is BaseContainerBlockEntity) {
                block.contents.forEachIndexed { idx, stack ->
                    applyStack(stack, StackHolder.ByBlock(bukkit, idx))
                }
            }
        }

        fun resolve() {
            Bukkit.getServer().worlds.forEach { world ->
                applyWorld(world)
                val level = (world as CraftWorld).handle
                world.handle.level.chunkSource.chunkMap.updatingChunks.visibleMap.forEach { (_, holder) ->
                    @Suppress("UNNECESSARY_SAFE_CALL") // it is necessary
                    holder.fullChunkNow?.let { chunk ->
                        applyChunk(chunk)
                        // TODO use a method which uses nms entities
                        level.getChunkEntities(chunk.locX, chunk.locZ).forEach { applyEntity(it) }
                        chunk.blockEntities.forEach { (_, block) -> applyBlock(world, block) }
                    }
                }
            }

            (Bukkit.getServer() as CraftServer).server.playerList.players.forEach { player ->
                applyStack(player.containerMenu.carried, StackHolder.ByCursor(player.bukkitEntity))
            }
        }
    }

    companion object {
        private val SLOTS_NMS = EquipmentSlot.values()
            .map { it to org.bukkit.inventory.EquipmentSlot.values()[it.ordinal] }
            .associate { it }
    }
}
*/
