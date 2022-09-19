package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.sokol.core.SokolEngine
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
        fun resolve(space: SokolEngine.Space)
    }

    lateinit var settings: Settings

    private val _lastStats = HashMap<String, TypeStats>()
    val lastStats: Map<String, TypeStats> get() = _lastStats
    
    val resolvers: MutableList<Resolver> = ArrayList()

    private val entityKey = sokol.persistence.entityKey.toString()

    internal fun load(settings: ConfigurationNode) {
        this.settings = settings.get { Settings() }
    }
    
    fun resolver(resolver: Resolver) {
        resolvers.add(resolver)
    }

    fun resolve(space: SokolEngine.Space) {
        _lastStats.clear()
        val server = (Bukkit.getServer() as CraftServer).server
        server.allLevels.forEach { resolveLevel(space, it) }
        server.playerList.players.forEach { player ->
            resolveItem(space, player.inventoryMenu.carried)
        }
        resolvers.forEach { it.resolve(space) }
    }

    private fun resolveTag(space: SokolEngine.Space, type: String, tag: CompoundTag?, callback: (Int, CompoundTag) -> Unit) {
        val stats = _lastStats.computeIfAbsent(type) { TypeStats() }
        stats.candidates++

        tag?.let {
            stats.updated++
            val wrappedTag = PaperCompoundTag(tag)
            val entity = sokol.persistence.readBlueprint(wrappedTag).create(space)
            space.addComponent(entity, NBTTagAccessor(wrappedTag))
            callback(entity, tag)
        }
    }

    private fun tagOf(pdc: PersistentDataContainer) = (pdc as CraftPersistentDataContainer).raw[entityKey] as? CompoundTag

    private fun resolveLevel(space: SokolEngine.Space, level: ServerLevel) {
        val world = level.world
        resolveTag(space, OBJECT_TYPE_WORLD, tagOf(level.world.persistentDataContainer)) { entity, _ ->
            space.addComponent(entity, hostedByWorld(world))
        }

        level.entities.all.forEach { resolveEntity(space, it) }

        level.chunkSource.chunkMap.updatingChunks.visibleMap.forEach { (_, holder) ->
            @Suppress("UNNECESSARY_SAFE_CALL") // this may be null
            holder.fullChunkNow?.let { resolveChunk(space, it, world) }
        }
    }

    private fun resolveChunk(space: SokolEngine.Space, chunk: LevelChunk, world: World) {
        resolveTag(space, OBJECT_TYPE_CHUNK, tagOf(chunk.bukkitChunk.persistentDataContainer)) { entity, _ ->
            space.addComponent(entity, hostedByChunk(chunk.bukkitChunk))
        }

        chunk.blockEntities.forEach { (pos, block) -> resolveBlock(space, block, pos, world) }
    }

    private fun resolveEntity(space: SokolEngine.Space, mob: Entity) {
        resolveTag(space, OBJECT_TYPE_ENTITY, tagOf(mob.bukkitEntity.persistentDataContainer)) { entity, _ ->
            val backing = lazy { mob.bukkitEntity }
            space.addComponent(entity, object : HostedByEntity {
                override val entity get() = backing.value
            })
        }

        when (mob) {
            is ItemEntity -> resolveItem(space, mob.item)
            is ItemFrame -> resolveItem(space, mob.item)
            is Player -> mob.inventory.contents.forEach { resolveItem(space, it) }
            is LivingEntity -> mob.armorSlots.forEach { resolveItem(space, it) }
        }
    }

    private fun resolveBlock(space: SokolEngine.Space, block: BlockEntity, pos: BlockPos, world: World) {
        resolveTag(space, OBJECT_TYPE_BLOCK, tagOf(block.persistentDataContainer)) { entity, _ ->
            val backing = lazy { world.getBlockAt(pos.x, pos.y, pos.z) }
            val state = lazy { backing.value.getState(false) }
            space.addComponent(entity, object : HostedByBlock {
                override val block get() = backing.value
                override val state get() = state.value
            })
        }

        when (block) {
            is BaseContainerBlockEntity -> block.contents.forEach { resolveItem(space, it) }
        }
    }

    private fun CompoundTag.compound(key: String) = tags[key] as? CompoundTag

    private fun CompoundTag.list(key: String) = tags[key] as? ListTag

    private fun resolveItemTagInternal(
        space: SokolEngine.Space,
        tagMeta: CompoundTag,
        getStack: () -> ItemStack,
        onDirtied: (org.bukkit.inventory.ItemStack) -> Unit
    ) {
        val bukkitStack = lazy { getStack().bukkitStack }
        val meta = lazy { bukkitStack.value.itemMeta }
        var dirty = false

        resolveTag(space, OBJECT_TYPE_ITEM, tagMeta.compound("PublicBukkitValues")?.compound(entityKey)) { entity, _ ->
            space.addComponent(entity, object : HostedByItem {
                override val stack get() = bukkitStack.value

                override fun <R> readMeta(action: (ItemMeta) -> R): R {
                    return action(meta.value)
                }

                override fun writeMeta(action: (ItemMeta) -> Unit) {
                    action(meta.value)
                    dirty = true
                }
            })
        }

        tagMeta.compound("BlockEntityTag")?.list("Items")?.forEach { tagItem ->
            (tagItem as? CompoundTag)?.let {
                resolveItemTag(space, tagItem) { dirtied ->
                    (dirtied as CraftItemStack).handle.save(tagItem)
                }
            }
        }

        if (dirty) {
            bukkitStack.value.itemMeta = meta.value
            onDirtied(bukkitStack.value)
        }
    }

    private fun resolveItemTag(space: SokolEngine.Space, tag: CompoundTag, onDirtied: (org.bukkit.inventory.ItemStack) -> Unit) {
        tag.compound("tag")?.let { tagMeta ->
            val stack = lazy { ItemStack.of(tag) }
            resolveItemTagInternal(space, tagMeta, { stack.value }, onDirtied)
        }
    }

    private fun resolveItem(space: SokolEngine.Space, item: ItemStack) {
        val tag = item.tag
        if (item.item === Items.AIR || tag == null) return
        resolveItemTagInternal(space, tag, { item }, {})
    }
}
