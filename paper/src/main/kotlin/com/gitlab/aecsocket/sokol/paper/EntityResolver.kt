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
import org.bukkit.block.Container
import org.bukkit.craftbukkit.v1_19_R2.CraftEquipmentSlot
import org.bukkit.craftbukkit.v1_19_R2.CraftServer
import org.bukkit.craftbukkit.v1_19_R2.persistence.CraftPersistentDataContainer
import org.bukkit.persistence.PersistentDataContainer
import java.lang.RuntimeException

private const val FLAGS = "flags"
const val FLAG_FORCE_UPDATE = 0x1

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

    private val log = sokol.log
    val entityKey = sokol.persistence.entityKey.toString()

    private val mobEntities = HashMap<Int, SokolEntity>()

    private val _lastStats = HashMap<SokolObjectType, TypeStats>()
    val lastStats: Map<SokolObjectType, TypeStats> get() = _lastStats

    private var lastSize: Int = 0

    private lateinit var mInTag: ComponentMapper<InTag>
    private lateinit var mIsWorld: ComponentMapper<IsWorld>
    private lateinit var mIsChunk: ComponentMapper<IsChunk>
    private lateinit var mIsBlock: ComponentMapper<IsBlock>
    private lateinit var mIsMob: ComponentMapper<IsMob>
    private lateinit var mIsItem: ComponentMapper<IsItem>
    private lateinit var mItemHolder: ComponentMapper<ItemHolder>
    private lateinit var mInItemTag: ComponentMapper<InItemTag>

    internal fun enable() {
        sokol.engine.apply {
            mInTag = mapper()
            mIsWorld = mapper()
            mIsChunk = mapper()
            mIsBlock = mapper()
            mIsMob = mapper()
            mIsItem = mapper()
            mItemHolder = mapper()
            mInItemTag = mapper()
        }

        sokol.onInput { event ->
            sokol.useSpace { space ->
                readPlayerItems(event.player).addAllInto(space)
                space.construct()
                space.call(event)
            }
        }
    }

    internal fun disable() {
        mobEntities.forEach { (_, entity) ->
            entity.write()
        }
    }

    fun readPDC(pdc: PersistentDataContainer): EntityBlueprint? {
        val tag = sokol.persistence.getTag(pdc, sokol.persistence.entityKey) ?: return null
        return sokol.persistence.readBlueprint(tag)
            .pushSet(mInTag) { InTag(tag) }
    }

    fun readItem(item: Item): EntityBlueprint? {
        if (!item.hasItemMeta()) return null
        val meta = item.itemMeta
        return readPDC(meta.persistentDataContainer)
            ?.pushSet(mIsItem) { IsItem({ item }, { meta }) }
    }

    fun readPlayerItems(player: org.bukkit.entity.Player): List<EntityBlueprint> {
        return player.inventory.mapIndexedNotNull { idx, item ->
            if (item == null) return@mapIndexedNotNull null
            readItem(item)
                ?.pushSet(mItemHolder) { ItemHolder.byPlayer(player, idx) }
        }
    }

    fun readMob(mob: Mob): EntityBlueprint? {
        if (!mob.isValid) return null
        return readPDC(mob.persistentDataContainer)
            ?.pushSet(mIsMob) { IsMob(mob) }
    }

    fun mobTrackedBy(entityId: Int) = mobEntities[entityId]

    fun mobTrackedBy(mob: Mob) = mobEntities[mob.entityId]

    fun trackMob(mob: Mob, entity: SokolEntity) {
        if (mobEntities.contains(mob.entityId))
            throw IllegalStateException("Already tracking mob $mob")
        mobEntities[mob.entityId] = entity
    }

    fun untrackMob(mob: Mob): SokolEntity? {
        return mobEntities.remove(mob.entityId)
    }

    fun resolve(callback: (EntitySpace) -> Unit) {
        _lastStats.clear()
        ResolveOperation(callback, _lastStats).resolve()
    }

    internal fun update() {
        val mobSpace = if (sokol.hasReloaded) {
            val mobSpace = sokol.engine.emptySpace(mobEntities.size)
            val newEntities = HashMap<Int, SokolEntity>()
            mobEntities.forEach { (key, entity) ->
                val mob = mIsMob.getOr(entity)?.mob ?: return@forEach
                sokol.useSpaceOf(entity) {} // write

                val newEntity = readMob(mob)?.create() ?: return@forEach
                newEntities[key] = newEntity
                mobSpace.add(newEntity)
            }

            // during reload, IsMob-using systems can still access the old entity via mobEntities
            mobSpace.construct()
            mobSpace.call(ReloadEvent)

            mobEntities.clear()
            mobEntities.putAll(newEntities)
            mobSpace
        } else sokol.engine.spaceOf(mobEntities.values)
        mobSpace.update()

        resolve { space ->
            space.construct()
            if (sokol.hasReloaded)
                space.call(ReloadEvent)
            space.update()
            space.write()
        }

        sokol.hasReloaded = false
    }

    private inner class ResolveOperation(
        val callback: (EntitySpace) -> Unit,
        val stats: MutableMap<SokolObjectType, TypeStats>,
    ) {
        private fun entityTagIn(pdc: PersistentDataContainer) = (pdc as CraftPersistentDataContainer).raw[entityKey] as? CompoundTag

        private fun Double.format() = "%.1f".format(this)

        private fun resolveTag(
            type: SokolObjectType,
            tag: CompoundTag?,
            entities: EntityCollection,
            function: EntityBlueprint.() -> Unit,
        ) {
            val typeStats = stats.computeIfAbsent(type) { TypeStats() }
            typeStats.candidates++
            if (tag == null) return

            typeStats.updated++
            val wrappedTag = PaperCompoundTag(tag)

            try {
                sokol.persistence.readBlueprint(wrappedTag)
                    .pushSet(mInTag) { InTag(wrappedTag) }
                    .also(function)
                    .addInto(entities)
            } catch (ex: PersistenceException) {
                throw EntityResolutionException("Could not read entities", ex)
            }
        }

        fun resolve() {
            val server = (Bukkit.getServer() as CraftServer).server
            val rootSpace = sokol.engine.emptySpace(lastSize + 64)

            server.allLevels.forEach { level ->
                resolveLevel(level, log, rootSpace)
            }

            callback(rootSpace)
            lastSize = rootSpace.size
        }

        private fun resolveLevel(level: ServerLevel, log: LogAcceptor, space: EntityCollection) {
            val bukkit = level.world
            val entityInfo = bukkit.name
            try {
                resolveTag(SokolObjectType.World, entityTagIn(level.world.persistentDataContainer), space) {
                    pushSet(mIsWorld) { IsWorld(bukkit) }
                }
            } catch (ex: EntityResolutionException) {
                log.line(LogLevel.Warning, ex) { "Could not resolve level $entityInfo" }
            }

            ChunkSystem.getVisibleChunkHolders(level).forEach { holder ->
                @Suppress("UNNECESSARY_SAFE_CALL") // this may be null
                holder.fullChunkNow?.let { chunk ->
                    resolveChunk(chunk, bukkit, ForwardingLogging(log) { "Level $entityInfo: $it" }, space)
                }
            }
        }

        private fun resolveChunk(chunk: LevelChunk, world: World, log: LogAcceptor, space: EntityCollection) {
            val bukkit = chunk.bukkitChunk
            try {
                resolveTag(SokolObjectType.Chunk, entityTagIn(bukkit.persistentDataContainer), space) {
                    pushSet(mIsChunk) { IsChunk(bukkit) }
                }
            } catch (ex: EntityResolutionException) {
                log.line(LogLevel.Warning, ex) { "Could not resolve chunk (${chunk.locX}, ${chunk.locZ})" }
            }

            chunk.blockEntities.forEach { (pos, block) ->
                resolveBlock(block, pos, world, log, space)
            }
        }

        private fun resolveBlock(block: BlockEntity, pos: BlockPos, world: World, log: LogAcceptor, space: EntityCollection) {
            val bukkit = world.getBlockAt(pos.x, pos.y, pos.z)
            val state = lazy { bukkit.getState(false) }

            try {
                resolveTag(SokolObjectType.Block, entityTagIn(block.persistentDataContainer), space) {
                    pushSet(mIsBlock) { IsBlock(bukkit) { state.value } }
                }
            } catch (ex: EntityResolutionException) {
                log.line(LogLevel.Warning, ex) { "Could not resolve block ${bukkit.type.key} @ (${pos.x}, ${pos.y}, ${pos.z})" }
            }

            when (block) {
                is BaseContainerBlockEntity -> {
                    if (sokol.settings.resolveContainerBlocks) {
                        val containerState = state.value as Container
                        val inventory = containerState.inventory
                        block.contents.forEachIndexed { idx, item ->
                            resolveItem(
                                item,
                                ItemHolder.byBlock(bukkit, inventory, idx),
                                ForwardingLogging(log) { "Block ${bukkit.type.key} @ (${pos.x}, ${pos.y}, ${pos.z}): $it" },
                                space
                            )
                        }
                    }
                }
            }
        }

        private fun resolveMob(mob: Entity, log: LogAcceptor, space: EntityCollection) {
            val bukkit = mob.bukkitEntity
            val pos = mob.position()
            val entityInfo = "${bukkit.name} (${mob.uuid}) @ (${pos.x.format()}, ${pos.y.format()}, ${pos.z.format()})"
            try {
                resolveTag(SokolObjectType.Mob, entityTagIn(bukkit.persistentDataContainer), space) {
                    pushSet(mIsMob) { IsMob(bukkit) }
                }
            } catch (ex: EntityResolutionException) {
                log.line(LogLevel.Warning, ex) { "Could not resolve mob $entityInfo" }
            }

            when (mob) {
                is ItemEntity -> {
                    resolveItem(
                        mob.item,
                        ItemHolder.byMob(bukkit),
                        ForwardingLogging(log) { "Mob $entityInfo: $it" },
                        space
                    )
                }
                is ItemFrame -> {
                    resolveItem(
                        mob.item,
                        ItemHolder.byMob(bukkit),
                        ForwardingLogging(log) { "Mob $entityInfo: $it" },
                        space
                    )
                }
                is Player -> {
                    val player = bukkit as org.bukkit.entity.Player
                    // items
                    resolveItem(
                        mob.inventoryMenu.carried,
                        ItemHolder.inCursor(player),
                        ForwardingLogging(log) { "Mob $entityInfo / cursor: $it" },
                        space
                    )
                    mob.inventory.contents.forEachIndexed { idx, item ->
                        resolveItem(
                            item,
                            ItemHolder.byPlayer(player, idx),
                            ForwardingLogging(log) { "Mob $entityInfo / slot $idx: $it" },
                            space
                        )
                    }
                }
                is LivingEntity -> {
                    EquipmentSlot.values().forEach { slot ->
                        val item = mob.getItemBySlot(slot)
                        resolveItem(
                            item,
                            ItemHolder.inEquipment(mob.bukkitLivingEntity, CraftEquipmentSlot.getSlot(slot)),
                            ForwardingLogging(log) { "Mob $entityInfo / slot $slot: $it" },
                            space
                        )
                    }
                }
            }
        }

        private fun CompoundTag.compound(key: String) = tags[key] as? CompoundTag

        private fun CompoundTag.list(key: String) = tags[key] as? ListTag

        private fun resolveItemTagInternal(
            tagMeta: CompoundTag,
            holder: ItemHolder,
            log: LogAcceptor,
            space: EntityCollection,
            allowUnforced: Boolean,
            getStack: () -> ItemStack,
            function: EntityBlueprint.() -> Unit = {},
        ) {
            val bukkit = lazy { getStack().bukkitStack }
            val meta = lazy { bukkit.value.itemMeta }

            val entityInfo = lazy { "${bukkit.value.type.key}" }
            val tagEntity = tagMeta.compound("PublicBukkitValues")?.compound(entityKey)

            if (allowUnforced || (tagEntity?.getInt(FLAGS) ?: 0) and FLAG_FORCE_UPDATE != 0) {
                try {
                    resolveTag(SokolObjectType.Item, tagEntity, space) {
                        pushSet(mIsItem) { IsItem({ bukkit.value }, { meta.value }) }
                        pushSet(mItemHolder) { holder }
                        also(function)
                    }
                } catch (ex: EntityResolutionException) {
                    log.line(LogLevel.Warning, ex) { "Could not resolve item ${entityInfo.value}" }
                }
            }

            if (sokol.settings.resolveContainerItems) {
                tagMeta.compound("BlockEntityTag")?.list("Items")?.forEach { tagChild ->
                    if (tagChild !is CompoundTag) return@forEach
                    val tagChildMeta = tagChild.compound("tag") ?: return@forEach
                    val slot = tagChild.getByte("Slot").toInt()

                    val childItem = lazy { ItemStack.of(tagChild) }
                    resolveItemTagInternal(
                        tagChildMeta,
                        ItemHolder.byContainerItem(holder, slot),
                        ForwardingLogging(log) { "Item ${entityInfo.value} / slot $slot: $it" },
                        space,
                        false,
                        { childItem.value }
                    ) {
                        pushSet(mInItemTag) { InItemTag(tagChild) }
                    }
                }
            }
        }

        private fun resolveItem(item: ItemStack, holder: ItemHolder, log: LogAcceptor, space: EntityCollection) {
            val tag = item.tag
            if (item.item === Items.AIR || tag == null) return
            resolveItemTagInternal(tag, holder, log, space, true, { item })
        }
    }
}
