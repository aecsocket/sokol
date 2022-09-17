package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.SokolEntity
import com.gitlab.aecsocket.sokol.core.SokolHost
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
import org.bukkit.craftbukkit.v1_19_R1.CraftServer
import org.bukkit.craftbukkit.v1_19_R1.persistence.CraftPersistentDataContainer
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

    // TODO hosts
    private fun resolveTag(callback: Callback, type: String, tag: CompoundTag?, getHost: () -> SokolHost = {
        object : SokolHost {}
    }) {
        val stats = _lastStats.computeIfAbsent(type) { TypeStats() }
        stats.candidates++

        tag?.let {
            stats.updated++
            val entity = sokol.persistence.readEntity(PaperCompoundTag(tag), getHost())
            callback(entity)
        }
    }

    private fun tagOf(pdc: PersistentDataContainer) = (pdc as CraftPersistentDataContainer).raw[entityKey] as? CompoundTag

    private fun resolveLevel(callback: Callback, level: ServerLevel) {
        resolveTag(callback, OBJECT_TYPE_WORLD, tagOf(level.world.persistentDataContainer))

        level.entities.all.forEach { resolveEntity(callback, it) }

        level.chunkSource.chunkMap.updatingChunks.visibleMap.forEach { (_, holder) ->
            @Suppress("UNNECESSARY_SAFE_CALL") // this may be null
            holder.fullChunkNow?.let { resolveChunk(callback, it) }
        }
    }

    private fun resolveChunk(callback: Callback, chunk: LevelChunk) {
        resolveTag(callback, OBJECT_TYPE_CHUNK, tagOf(chunk.bukkitChunk.persistentDataContainer))

        chunk.blockEntities.forEach { (pos, block) -> resolveBlock(callback, block, pos) }
    }

    private fun resolveEntity(callback: Callback, mob: Entity) {
        resolveTag(callback, OBJECT_TYPE_ENTITY, tagOf(mob.bukkitEntity.persistentDataContainer))

        when (mob) {
            is ItemEntity -> resolveItem(callback, mob.item)
            is ItemFrame -> resolveItem(callback, mob.item)
            is Player -> mob.inventory.contents.forEach { resolveItem(callback, it) }
            is LivingEntity -> mob.armorSlots.forEach { resolveItem(callback, it) }
        }
    }

    private fun resolveBlock(callback: Callback, block: BlockEntity, pos: BlockPos) {
        resolveTag(callback, OBJECT_TYPE_BLOCK, tagOf(block.persistentDataContainer))

        if (block is BaseContainerBlockEntity) {
            block.contents.forEach { resolveItem(callback, it) }
        }
    }

    private fun resolveItem(callback: Callback, item: ItemStack) {
        if (item.item === Items.AIR) return

        // TODO this needs to not use itemMeta unless absolutely necessary
        resolveTag(callback, OBJECT_TYPE_ITEM, tagOf(item.bukkitStack.itemMeta.persistentDataContainer))

        fun CompoundTag.compound(key: String) = tags[key] as? CompoundTag

        fun CompoundTag.list(key: String) = tags[key] as? ListTag

        item.tag?.compound("BlockEntityTag")?.list("Items")?.forEach { tagItem ->
            // shulker box items
            if (tagItem is CompoundTag) {
                // TODO resolve item by tag
                // TODO oh my god optimize this to check tag first
                // and only make the stack later
                resolveItem(callback, ItemStack.of(tagItem))

                /*
                tagItem.compound("tag")?.let { tagItemMeta ->
                }*/
            }
        }
    }
}
