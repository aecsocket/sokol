package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.core.ForwardingLogging
import com.gitlab.aecsocket.alexandria.core.LogAcceptor
import com.gitlab.aecsocket.alexandria.core.LogLevel
import com.gitlab.aecsocket.sokol.core.*
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
import org.bukkit.World
import org.bukkit.block.BlockState
import org.bukkit.block.Container
import org.bukkit.craftbukkit.v1_19_R1.CraftEquipmentSlot
import org.bukkit.craftbukkit.v1_19_R1.CraftServer
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack
import org.bukkit.craftbukkit.v1_19_R1.persistence.CraftPersistentDataContainer
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataContainer
import java.lang.RuntimeException

interface SokolObjectType {
    val key: String

    companion object {
        val World = objectTypeOf("world")
        val Chunk = objectTypeOf("chunk")
        val Mob = objectTypeOf("mob")
        val Block = objectTypeOf("block")
        val Item = objectTypeOf("item")
    }
}

fun objectTypeOf(key: String) = object : SokolObjectType {
    override val key get() = key
}

class EntityResolutionException(message: String? = null, cause: Throwable? = null)
    : RuntimeException(message, cause)

class EntityResolver internal constructor(
    private val sokol: Sokol
) {
    data class TypeStats(
        var candidates: Int = 0,
        var updated: Int = 0,
    )

    fun interface InputHandler {
        fun handle(event: PlayerInput)
    }

    private val log = sokol.log
    private val entityKey = sokol.persistence.entityKey.toString()

    private val _lastStats = HashMap<SokolObjectType, TypeStats>()
    val lastStats: Map<SokolObjectType, TypeStats> get() = _lastStats

    private lateinit var mWorld: ComponentMapper<HostedByWorld>
    private lateinit var mChunk: ComponentMapper<HostedByChunk>
    private lateinit var mBlock: ComponentMapper<HostedByBlock>
    private lateinit var mMob: ComponentMapper<HostedByMob>
    private lateinit var mItem: ComponentMapper<HostedByItem>
    private lateinit var mItemHolder: ComponentMapper<ItemHolder>

    internal fun enable() {
        mWorld = sokol.engine.componentMapper()
        mChunk = sokol.engine.componentMapper()
        mBlock = sokol.engine.componentMapper()
        mMob = sokol.engine.componentMapper()
        mItem = sokol.engine.componentMapper()
        mItemHolder = sokol.engine.componentMapper()

        sokol.inputHandler { event ->
            // even though we're off-main, we still write here
            sokol.usePlayerItems(event.player) { entity ->
                entity.call(event)
            }
        }
    }

    fun resolve(callback: (SokolEntity) -> Unit) {
        _lastStats.clear()
        ResolveOperation(callback, _lastStats).resolve()
    }

    private inner class ResolveOperation(
        val callback: (SokolEntity) -> Unit,
        val stats: MutableMap<SokolObjectType, TypeStats>,
    ) {
        private fun tagOf(pdc: PersistentDataContainer) = (pdc as CraftPersistentDataContainer).raw[entityKey] as? CompoundTag

        private fun Double.format() = "%.1f".format(this)

        private fun resolveTag(
            type: SokolObjectType,
            tag: CompoundTag?,
            builder: (EntityBlueprint) -> Unit = {}
        ) {
            val typeStats = stats.computeIfAbsent(type) { TypeStats() }
            typeStats.candidates++

            tag?.let {
                typeStats.updated++
                val wrappedTag = PaperCompoundTag(tag)
                val blueprint = try {
                    sokol.persistence.readBlueprint(wrappedTag)
                } catch (ex: PersistenceException) {
                    throw EntityResolutionException("Could not read blueprint", ex)
                }

                builder(blueprint)
                val entity = try {
                    sokol.engine.buildEntity(blueprint)
                } catch (ex: Exception) {
                    throw EntityResolutionException("Could not build entity", ex)
                }

                try {
                    callback(entity)
                } catch (ex: Exception) {
                    throw EntityResolutionException("Could not run callback", ex)
                }

                // DONE implement component deltas system, which actually write only what changed
                // TODO bitmask to determine whether entities even need to have Update called on them
                // TODO entity update calls can be batched to have components be close together in memory

                try {
                    sokol.persistence.writeEntityDeltas(entity, wrappedTag)
                } catch (ex: Exception) {
                    throw EntityResolutionException("Could not write entity to tag", ex)
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
                resolveTag(SokolObjectType.World, tagOf(level.world.persistentDataContainer)) { blueprint ->
                    mWorld.set(blueprint, hostedByWorld(bukkit))
                }
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
                resolveTag(SokolObjectType.Chunk, tagOf(bukkit.persistentDataContainer)) { blueprint ->
                    mChunk.set(blueprint, hostedByChunk(bukkit))
                }
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
                resolveTag(SokolObjectType.Mob, tagOf(bukkit.persistentDataContainer)) { blueprint ->
                    mMob.set(blueprint, hostedByMob(bukkit))
                }
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
                    // items
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
                    tagOf(block.persistentDataContainer)
                ) { blueprint ->
                    mBlock.set(blueprint, object : HostedByBlock {
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
                    tagEntity
                ) { blueprint ->
                    mItem.set(blueprint, object : HostedByItem {
                        override val item get() = bukkit.value

                        override fun <R> readMeta(action: (ItemMeta) -> R): R {
                            return action(meta.value)
                        }

                        override fun writeMeta(action: (ItemMeta) -> Unit) {
                            action(meta.value)
                            dirty = true
                        }
                    })

                    mItemHolder.set(blueprint, holder)
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
