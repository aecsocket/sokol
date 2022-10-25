package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.core.ForwardingLogging
import com.gitlab.aecsocket.alexandria.core.LogAcceptor
import com.gitlab.aecsocket.alexandria.core.LogLevel
import com.gitlab.aecsocket.sokol.core.SokolEntityAccess
import com.gitlab.aecsocket.sokol.core.SokolEntityBuilder
import com.gitlab.aecsocket.sokol.paper.component.*
import io.papermc.paper.chunk.system.ChunkSystem
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ItemFrame
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.chunk.LevelChunk
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.block.Container
import org.bukkit.craftbukkit.v1_19_R1.CraftEquipmentSlot
import org.bukkit.craftbukkit.v1_19_R1.CraftServer
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack
import org.bukkit.craftbukkit.v1_19_R1.persistence.CraftPersistentDataContainer
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataContainer
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import java.lang.RuntimeException

private typealias Mob = org.bukkit.entity.Entity
private typealias Item = org.bukkit.inventory.ItemStack

data class BlockData(
    val block: Block,
    val getState: () -> BlockState,
)

data class ItemData(
    val getItem: () -> Item,
    val getMeta: () -> ItemMeta,
)

interface SokolObjectType<T> {
    val key: String

    companion object {
        val World = objectTypeOf<World>("world")
        val Chunk = objectTypeOf<Chunk>("chunk")
        val Mob = objectTypeOf<Mob>("mob")
        val Block = objectTypeOf<BlockData>("block")
        val Item = objectTypeOf<ItemData>("item")
    }
}

inline fun <reified T> objectTypeOf(key: String) = object : SokolObjectType<T> {
    override val key get() = key
}

class EntityResolutionException(message: String? = null, cause: Throwable? = null)
    : RuntimeException(message, cause)

class EntityResolver internal constructor(
    private val sokol: Sokol
) {
    @ConfigSerializable
    data class Settings(
        val a: Boolean = false
    )

    data class TypeStats(
        var candidates: Int = 0,
        var updated: Int = 0,
    )

    fun interface Populator<T> {
        fun populate(builder: SokolEntityBuilder, obj: T)
    }

    private val log = sokol.log
    lateinit var settings: Settings

    private val _lastStats = HashMap<SokolObjectType<*>, TypeStats>()
    val lastStats: Map<SokolObjectType<*>, TypeStats> get() = _lastStats

    private val populators = HashMap<SokolObjectType<*>, MutableList<Populator<*>>>()
    private val entityKey = sokol.persistence.entityKey.toString()

    internal fun load(settings: ConfigurationNode) {
        this.settings = settings.get { Settings() }
    }

    fun <T> populator(objectType: SokolObjectType<T>, populator: Populator<T>) {
        populators.computeIfAbsent(objectType) { ArrayList() }.add(populator)
    }

    fun <T> populate(objectType: SokolObjectType<T>, builder: SokolEntityBuilder, obj: T): SokolEntityBuilder {
        @Suppress("UNCHECKED_CAST")
        val populators: List<Populator<T>> = populators[objectType] as? List<Populator<T>> ?: return builder
        populators.forEach {
            it.populate(builder, obj)
        }
        return builder
    }

    fun resolve(callback: (SokolEntityAccess) -> Unit) {
        _lastStats.clear()
        ResolveOperation(callback, _lastStats).resolve()
    }

    private inner class ResolveOperation(
        val callback: (SokolEntityAccess) -> Unit,
        val stats: MutableMap<SokolObjectType<*>, TypeStats>,
    ) {
        private fun tagOf(pdc: PersistentDataContainer) = (pdc as CraftPersistentDataContainer).raw[entityKey] as? CompoundTag

        private fun Double.format() = "%.1f".format(this)

        private fun <T> resolveTag(
            type: SokolObjectType<T>,
            obj: T,
            tag: CompoundTag?,
            consumer: (SokolEntityBuilder) -> Unit = {}
        ) {
            val typeStats = stats.computeIfAbsent(type) { TypeStats() }
            typeStats.candidates++

            tag?.let {
                typeStats.updated++
                val wrappedTag = PaperCompoundTag(tag)
                val blueprint = try {
                    sokol.persistence.readBlueprint(wrappedTag)
                } catch (ex: IllegalArgumentException) {
                    throw EntityResolutionException("Could not read blueprint", ex)
                }

                if (!blueprint.isEmpty()) {
                    val builder = blueprint.build(sokol.engine)
                    populate(type, builder, obj)
                    consumer(builder)
                    val entity = builder.build()

                    try {
                        callback(entity)
                    } catch (ex: Exception) {
                        throw EntityResolutionException("Could not run callback", ex)
                    }

                    // TODO high priority: only reserialize components into the tag if it's actually changed (been dirtied)
                    // how to implement this? idfk
                    try {
                        sokol.persistence.writeEntity(entity, wrappedTag)
                    } catch (ex: Exception) {
                        throw EntityResolutionException("Could not write entity to tag", ex)
                    }
                }
            }
        }

        fun resolve() {
            val server = (Bukkit.getServer() as CraftServer).server
            server.allLevels.forEach { level ->
                resolveLevel(level, log)
            }
        }

        private fun resolveLevel(level: ServerLevel, log: LogAcceptor) {
            val bukkit = level.world
            val entityInfo = bukkit.name
            try {
                resolveTag(SokolObjectType.World, bukkit, tagOf(level.world.persistentDataContainer))
            } catch (ex: EntityResolutionException) {
                log.line(LogLevel.Warning, ex) { "Could not resolve level $entityInfo" }
            }

            level.entities.all.forEach { mob ->
                resolveMob(mob, ForwardingLogging(log) { "Level $entityInfo: $it" })
            }

            ChunkSystem.getVisibleChunkHolders(level).forEach { holder ->
                @Suppress("UNNECESSARY_SAFE_CALL") // this may be null
                holder.fullChunkNow?.let { chunk ->
                    resolveChunk(chunk, bukkit, ForwardingLogging(log) { "Level $entityInfo: $it" })
                }
            }
        }

        private fun resolveChunk(chunk: LevelChunk, world: World, log: LogAcceptor) {
            val bukkit = chunk.bukkitChunk
            try {
                resolveTag(SokolObjectType.Chunk, bukkit, tagOf(bukkit.persistentDataContainer))
            } catch (ex: EntityResolutionException) {
                log.line(LogLevel.Warning, ex) { "Could not resolve chunk (${chunk.locX}, ${chunk.locZ})" }
            }

            chunk.blockEntities.forEach { (pos, block) ->
                resolveBlock(block, pos, world, log)
            }
        }

        private fun resolveMob(mob: Entity, log: LogAcceptor) {
            val bukkit = mob.bukkitEntity
            val pos = mob.position()
            val entityInfo = "${bukkit.name} (${mob.uuid}) @ (${pos.x.format()}, ${pos.y.format()}, ${pos.z.format()})"
            try {
                resolveTag(SokolObjectType.Mob, bukkit, tagOf(bukkit.persistentDataContainer))
            } catch (ex: EntityResolutionException) {
                log.line(LogLevel.Warning, ex) { "Could not resolve mob $entityInfo" }
            }

            when (mob) {
                is ItemEntity -> {
                    resolveItem(mob.item, ItemHolder.byMob(bukkit), ForwardingLogging(log) { "Mob $entityInfo: $it" })
                }
                is ItemFrame -> {
                    resolveItem(mob.item, ItemHolder.byMob(bukkit), ForwardingLogging(log) { "Mob $entityInfo: $it" })
                }
                is Player -> {
                    val player = bukkit as org.bukkit.entity.Player
                    resolveItem(
                        mob.inventoryMenu.carried,
                        ItemHolder.inCursor(player),
                        ForwardingLogging(log) { "Mob $entityInfo / cursor: $it" }
                    )
                    mob.inventory.contents.forEachIndexed { idx, item ->
                        resolveItem(
                            item,
                            ItemHolder.byPlayer(player, idx),
                            ForwardingLogging(log) { "Mob $entityInfo / slot $idx: $it" }
                        )
                    }
                }
                is LivingEntity -> {
                    EquipmentSlot.values().forEach { slot ->
                        val item = mob.getItemBySlot(slot)
                        resolveItem(
                            item,
                            ItemHolder.inEquipment(mob.bukkitLivingEntity, CraftEquipmentSlot.getSlot(slot)),
                            ForwardingLogging(log) { "Mob $entityInfo / slot $slot: $it" }
                        )
                    }
                }
            }
        }

        private fun resolveBlock(block: BlockEntity, pos: BlockPos, world: World, log: LogAcceptor) {
            val bukkit = world.getBlockAt(pos.x, pos.y, pos.z)
            val state = lazy { bukkit.getState(false) }
            var dirty = false

            try {
                resolveTag(
                    SokolObjectType.Block,
                    BlockData(bukkit) { state.value },
                    tagOf(block.persistentDataContainer)
                ) { builder ->
                    builder.setComponent(object : HostedByBlock {
                        override val block get() = bukkit

                        override fun <R> readState(action: (BlockState) -> R): R {
                            return action(state.value)
                        }

                        override fun writeState(action: (BlockState) -> Unit) {
                            action(state.value)
                            dirty = true
                        }
                    })
                }
            } catch (ex: EntityResolutionException) {
                log.line(LogLevel.Warning, ex) { "Could not resolve block ${bukkit.type.key} @ (${pos.x}, ${pos.y}, ${pos.z})" }
            }

            when (block) {
                is BaseContainerBlockEntity -> {
                    val containerState = state.value as Container
                    val inventory = containerState.inventory
                    block.contents.forEachIndexed { idx, item ->
                        resolveItem(
                            item,
                            ItemHolder.byBlock(bukkit, inventory, idx),
                            ForwardingLogging(log) { "Block ${bukkit.type.key} @ (${pos.x}, ${pos.y}, ${pos.z}): $it" }
                        )
                    }
                }
            }

            if (dirty) {
                state.value.update()
            }
        }

        private fun CompoundTag.compound(key: String) = tags[key] as? CompoundTag

        private fun CompoundTag.list(key: String) = tags[key] as? ListTag

        private fun resolveItemTagInternal(
            tagMeta: CompoundTag,
            holder: ItemHolder,
            log: LogAcceptor,
            getStack: () -> ItemStack,
            onDirtied: (Item) -> Unit
        ) {
            val bukkit = lazy { getStack().bukkitStack }
            val meta = lazy { bukkit.value.itemMeta }
            var dirty = false

            val tagEntity = tagMeta.compound("PublicBukkitValues")?.compound(entityKey)
            val entityInfo = lazy { "${bukkit.value.type.key}" }
            try {
                resolveTag(
                    SokolObjectType.Item,
                    ItemData({ bukkit.value }, { meta.value }),
                    tagEntity
                ) { builder ->
                    builder.setComponent(object : HostedByItem {
                        override val item get() = bukkit.value

                        override fun <R> readMeta(action: (ItemMeta) -> R): R {
                            return action(meta.value)
                        }

                        override fun writeMeta(action: (ItemMeta) -> Unit) {
                            action(meta.value)
                            dirty = true
                        }
                    })
                    builder.setComponent(holder)
                }
            } catch (ex: EntityResolutionException) {
                log.line(LogLevel.Warning, ex) { "Could not resolve item ${entityInfo.value}" }
            }

            tagMeta.compound("BlockEntityTag")?.list("Items")?.forEach { tagItem ->
                (tagItem as? CompoundTag)?.let {
                    val slot = tagItem.getByte("Slot").toInt()
                    resolveItemTag(
                        tagItem,
                        ItemHolder.byContainerItem(holder, slot),
                        ForwardingLogging(log) { "Item ${entityInfo.value} / slot $slot: $it" }
                    ) { dirtied ->
                        (dirtied as CraftItemStack).handle.save(tagItem)
                    }
                }
            }

            if (dirty) {
                val iStack = bukkit.value
                val iMeta = meta.value

                // our updated entity tag gets written into the meta
                // then this meta is set to the stack
                tagEntity?.let {
                    (iMeta.persistentDataContainer as CraftPersistentDataContainer).raw[entityKey] = tagEntity
                }

                iStack.itemMeta = iMeta
                onDirtied(iStack)
            }
        }

        private fun resolveItemTag(tag: CompoundTag, holder: ItemHolder, log: LogAcceptor, onDirtied: (Item) -> Unit) {
            tag.compound("tag")?.let { tagMeta ->
                val stack = lazy { ItemStack.of(tag) }
                resolveItemTagInternal(tagMeta, holder, log, { stack.value }, onDirtied)
            }
        }

        private fun resolveItem(item: ItemStack, holder: ItemHolder, log: LogAcceptor) {
            val tag = item.tag
            if (item.item === Items.AIR || tag == null) return
            resolveItemTagInternal(tag, holder, log, { item }) {}
        }
    }
}
