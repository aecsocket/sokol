package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.sokol.core.SokolEntity
import com.gitlab.aecsocket.sokol.core.TIMING_MAX_MEASUREMENTS
import com.gitlab.aecsocket.sokol.core.Timings
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
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
import org.bukkit.craftbukkit.v1_19_R1.CraftServer
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack
import org.bukkit.craftbukkit.v1_19_R1.persistence.CraftPersistentDataContainer
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataContainer
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.objectmapping.ConfigSerializable

const val OBJECT_TYPE_WORLD = "world"
const val OBJECT_TYPE_CHUNK = "chunk"
const val OBJECT_TYPE_ENTITY = "entity"
const val OBJECT_TYPE_BLOCK = "block"
const val OBJECT_TYPE_ITEM = "item"

typealias Callback = (SokolEntity) -> Unit

class EntityResolver(
    private val sokol: Sokol,
) {
    @ConfigSerializable
    data class Settings(
        val a: Boolean = false
    )

    data class TypeStats(
        var candidates: Int = 0,
        var updated: Int = 0,
    )

    fun interface Resolver {
        fun resolve(callback: Callback)
    }

    lateinit var settings: Settings

    private val _lastStats = HashMap<String, TypeStats>()
    val lastStats: Map<String, TypeStats> get() = _lastStats

    val timings = Timings(TIMING_MAX_MEASUREMENTS)
    
    val resolvers: MutableList<Resolver> = ArrayList()

    private val entityKey = sokol.persistence.entityKey.toString()

    internal fun load(settings: ConfigurationNode) {
        this.settings = settings.get { Settings() }
    }
    
    fun resolver(resolver: Resolver) {
        resolvers.add(resolver)
    }

    fun resolve(callback: Callback) {
        _lastStats.clear()

        timings.time {
            val server = (Bukkit.getServer() as CraftServer).server
            server.allLevels.forEach { resolveLevel(callback, it) }
            server.playerList.players.forEach { player ->
                resolveItem(callback, player.inventoryMenu.carried)
            }
            resolvers.forEach { it.resolve(callback) }
        }
    }

    private fun resolveTag(type: String, tag: CompoundTag?, callback: (SokolEntity, CompoundTag) -> Unit) {
        val stats = _lastStats.computeIfAbsent(type) { TypeStats() }
        stats.candidates++

        tag?.let {
            stats.updated++
            val entity = sokol.persistence.readEntity(PaperCompoundTag(tag))
            callback(entity, tag)
        }
    }

    private fun tagOf(pdc: PersistentDataContainer) = (pdc as CraftPersistentDataContainer).raw[entityKey] as? CompoundTag

    private fun resolveLevel(callback: Callback, level: ServerLevel) {
        val world = level.world
        resolveTag(OBJECT_TYPE_WORLD, tagOf(level.world.persistentDataContainer)) { entity, _ ->
            entity.add(object : HostedByWorld {
                override val world get() = world
            })
            callback(entity)
        }

        level.entities.all.forEach { resolveEntity(callback, it) }

        level.chunkSource.chunkMap.updatingChunks.visibleMap.forEach { (_, holder) ->
            @Suppress("UNNECESSARY_SAFE_CALL") // this may be null
            holder.fullChunkNow?.let { resolveChunk(callback, it, world) }
        }
    }

    // TODO save the data after callback!!! (like in entity)
    private fun resolveChunk(callback: Callback, chunk: LevelChunk, world: World) {
        resolveTag(OBJECT_TYPE_CHUNK, tagOf(chunk.bukkitChunk.persistentDataContainer)) { entity, _ ->
            entity.add(object : HostedByChunk {
                override val chunk get() = chunk.bukkitChunk
            })
            callback(entity)
        }

        chunk.blockEntities.forEach { (pos, block) -> resolveBlock(callback, block, pos, world) }
    }

    private fun resolveEntity(callback: Callback, mob: Entity) {
        resolveTag(OBJECT_TYPE_ENTITY, tagOf(mob.bukkitEntity.persistentDataContainer)) { entity, tag ->
            val backing = lazy { mob.bukkitEntity }
            entity.add(object : HostedByEntity {
                override val entity get() = backing.value
            })
            callback(entity)

            val wrappedTag = PaperCompoundTag(tag)
            sokol.persistence.writeEntity(entity, wrappedTag)
            sokol.persistence.writeTagTo(wrappedTag, sokol.persistence.entityKey, backing.value.persistentDataContainer)
        }

        when (mob) {
            is ItemEntity -> resolveItem(callback, mob.item)
            is ItemFrame -> resolveItem(callback, mob.item)
            is Player -> mob.inventory.contents.forEach { resolveItem(callback, it) }
            is LivingEntity -> mob.armorSlots.forEach { resolveItem(callback, it) }
        }
    }

    private fun resolveBlock(callback: Callback, block: BlockEntity, pos: BlockPos, world: World) {
        resolveTag(OBJECT_TYPE_BLOCK, tagOf(block.persistentDataContainer)) { entity, _ ->
            val backing = lazy { world.getBlockAt(pos.x, pos.y, pos.z) }
            val state = lazy { backing.value.getState(false) }
            entity.add(object : HostedByBlock {
                override val block get() = backing.value
                override val state get() = state.value
            })
            callback(entity)
        }

        when (block) {
            is BaseContainerBlockEntity -> block.contents.forEach { resolveItem(callback, it) }
        }
    }

    private fun CompoundTag.compound(key: String) = tags[key] as? CompoundTag

    private fun CompoundTag.list(key: String) = tags[key] as? ListTag

    private fun resolveItemTagInternal(
        callback: Callback,
        tagMeta: CompoundTag,
        getStack: () -> ItemStack,
        onDirtied: (org.bukkit.inventory.ItemStack) -> Unit
    ) {
        val bukkitStack = lazy { getStack().bukkitStack }
        val meta = lazy { bukkitStack.value.itemMeta }
        var dirty = false

        resolveTag(OBJECT_TYPE_ITEM, tagMeta.compound("PublicBukkitValues")?.compound(entityKey)) { entity, _ ->
            entity.add(object : HostedByItem {
                override val stack get() = bukkitStack.value

                override fun <R> readMeta(action: (ItemMeta) -> R): R {
                    return action(meta.value)
                }

                override fun writeMeta(action: (ItemMeta) -> Unit) {
                    action(meta.value)
                    dirty = true
                }
            })
            callback(entity)
        }

        tagMeta.compound("BlockEntityTag")?.list("Items")?.forEach { tagItem ->
            (tagItem as? CompoundTag)?.let {
                resolveItemTag(callback, tagItem) { dirtied ->
                    (dirtied as CraftItemStack).handle.save(tagItem)
                }
            }
        }

        if (dirty) {
            bukkitStack.value.itemMeta = meta.value
            onDirtied(bukkitStack.value)
        }
    }

    private fun resolveItemTag(callback: Callback, tag: CompoundTag, onDirtied: (org.bukkit.inventory.ItemStack) -> Unit) {
        tag.compound("tag")?.let { tagMeta ->
            val stack = lazy { ItemStack.of(tag) }
            resolveItemTagInternal(callback, tagMeta, { stack.value }, onDirtied)
        }
    }

    private fun resolveItem(callback: Callback, item: ItemStack) {
        val tag = item.tag
        if (item.item === Items.AIR || tag == null) return
        resolveItemTagInternal(callback, tag, { item }, {})
    }
}
