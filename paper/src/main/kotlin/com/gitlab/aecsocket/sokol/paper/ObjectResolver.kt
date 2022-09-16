package com.gitlab.aecsocket.sokol.paper

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
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.objectmapping.ConfigSerializable

const val OBJECT_TYPE_WORLD = "world"
const val OBJECT_TYPE_CHUNK = "chunk"
const val OBJECT_TYPE_ENTITY = "entity"
const val OBJECT_TYPE_BLOCK = "block"
const val OBJECT_TYPE_ITEM = "item"

private const val MAX_RESOLVE_TIMES = 60 * 20

class ObjectResolver(
    private val sokol: Sokol,
    var settings: Settings = Settings()
) {
    @ConfigSerializable
    data class Settings(
        val a: Boolean = false
    )

    data class TypeStats(
        var candidates: Int = 0,
        var updated: Int = 0,
    )

    private val _lastStats = HashMap<String, TypeStats>()
    val lastStats: Map<String, TypeStats> get() = _lastStats

    private val _resolveTimes = ArrayList<Long>()
    val resolveTimes: List<Long> get() = _resolveTimes

    internal fun load(settings: ConfigurationNode) {
        this.settings = settings.get { Settings() }
    }

    fun resolve() {
        _lastStats.clear()
        val start = System.currentTimeMillis()
        (Bukkit.getServer() as CraftServer).server.allLevels.forEach { resolveLevel(it) }
        val resolveTime = System.currentTimeMillis() - start

        _resolveTimes.add(resolveTime)
        while (_resolveTimes.size > MAX_RESOLVE_TIMES) {
            _resolveTimes.removeAt(0)
        }
    }

    private fun resolveObject(type: String, obj: SokolObject) {
        val stats = _lastStats.computeIfAbsent(type) { TypeStats() }
        stats.candidates++


    }

    private fun resolveLevel(level: ServerLevel) {
        resolveObject(OBJECT_TYPE_WORLD, object : SokolObject {})

        level.entities.all.forEach { resolveEntity(it) }

        level.chunkSource.chunkMap.updatingChunks.visibleMap.forEach { (_, holder) ->
            @Suppress("UNNECESSARY_SAFE_CALL") // this may be null
            holder.fullChunkNow?.let { resolveChunk(it) }
        }
    }

    private fun resolveChunk(chunk: LevelChunk) {
        resolveObject(OBJECT_TYPE_CHUNK, object : SokolObject {})

        chunk.blockEntities.forEach { (pos, block) -> resolveBlock(block, pos) }
    }

    private fun resolveEntity(mob: Entity) {
        resolveObject(OBJECT_TYPE_ENTITY, object : SokolObject {})

        when (mob) {
            is ItemEntity -> resolveItem(mob.item)
            is ItemFrame -> resolveItem(mob.item)
            is Player -> mob.inventory.contents.forEach { resolveItem(it) }
            is LivingEntity -> mob.armorSlots.forEach { resolveItem(it) }
        }
    }

    private fun resolveBlock(block: BlockEntity, pos: BlockPos) {
        resolveObject(OBJECT_TYPE_BLOCK, object : SokolObject {})

        if (block is BaseContainerBlockEntity) {
            block.contents.forEach { resolveItem(it) }
        }
    }

    private fun resolveItem(item: ItemStack) {
        if (item.item === Items.AIR) return

        resolveObject(OBJECT_TYPE_ITEM, object : SokolObject {})

        fun CompoundTag.compound(key: String) = tags[key] as? CompoundTag

        fun CompoundTag.list(key: String) = tags[key] as? ListTag

        item.tag?.compound("BlockEntityTag")?.list("Items")?.forEach { tagItem ->
            if (tagItem is CompoundTag) {
                // TODO resolve item by tag
                // TODO oh my god optimize this to check tag first
                // and only make the stack later
                resolveItem(ItemStack.of(tagItem))

                /*
                tagItem.compound("tag")?.let { tagItemMeta ->
                }*/
            }
        }
    }
}
