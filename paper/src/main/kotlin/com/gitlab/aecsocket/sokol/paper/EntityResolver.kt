package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.sokol.core.SokolEntityAccess
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

private typealias Mob = org.bukkit.entity.Entity
private typealias BukkitStack = org.bukkit.inventory.ItemStack

data class ItemInstance(
    val stack: BukkitStack,
    val meta: ItemMeta,
)

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
        fun populate(entity: SokolEntityAccess, backing: T)
    }

    lateinit var settings: Settings

    private val _lastStats = HashMap<String, TypeStats>()
    val lastStats: Map<String, TypeStats> get() = _lastStats

    private val mobPopulators = ArrayList<Populator<Mob>>()
    private val itemPopulators = ArrayList<Populator<ItemInstance>>()
    private val entityKey = sokol.persistence.entityKey.toString()

    internal fun load(settings: ConfigurationNode) {
        this.settings = settings.get { Settings() }
    }

    fun mobPopulator(populator: Populator<Mob>) {
        mobPopulators.add(populator)
    }

    fun itemPopulator(populator: Populator<ItemInstance>) {
        itemPopulators.add(populator)
    }

    fun populate(entity: SokolEntityAccess, mob: Mob) {
        mobPopulators.forEach { it.populate(entity, mob) }
    }

    fun populate(entity: SokolEntityAccess, stack: BukkitStack, meta: ItemMeta) {
        val instance = ItemInstance(stack, meta)
        itemPopulators.forEach { it.populate(entity, instance) }
    }

    fun resolve(callback: (SokolEntityAccess) -> Unit) {
        _lastStats.clear()
        ResolveOperation(callback, _lastStats).resolve()
    }

    private inner class ResolveOperation(
        val callback: (SokolEntityAccess) -> Unit,
        val stats: MutableMap<String, TypeStats>,
    ) {
        fun resolve() {
            val server = (Bukkit.getServer() as CraftServer).server
            server.allLevels.forEach { resolveLevel(it) }
            server.playerList.players.forEach { player ->
                resolveItem(player.inventoryMenu.carried)
            }
        }

        private fun resolveTag(type: String, tag: CompoundTag?, populator: (SokolEntityAccess) -> Unit) {
            val typeStats = stats.computeIfAbsent(type) { TypeStats() }
            typeStats.candidates++

            tag?.let {
                typeStats.updated++
                val wrappedTag = PaperCompoundTag(tag)
                val blueprint = sokol.persistence.readBlueprint(wrappedTag)
                if (!blueprint.isEmpty()) {
                    val entity = blueprint.create(sokol.engine)
                    populator(entity)
                    callback(entity)
                    // TODO high priority: only reserialize components into the tag if it's actually changed (been dirtied)
                    // how to implement this? idfk
                    sokol.persistence.writeEntity(entity, wrappedTag)
                }
            }
        }

        private fun tagOf(pdc: PersistentDataContainer) = (pdc as CraftPersistentDataContainer).raw[entityKey] as? CompoundTag

        private fun resolveLevel(level: ServerLevel) {
            val bukkit = level.world
            resolveTag(OBJECT_TYPE_WORLD, tagOf(level.world.persistentDataContainer)) { entity ->
                entity.addComponent(hostedByWorld(bukkit))
            }

            level.entities.all.forEach { resolveMob(it) }

            level.chunkSource.chunkMap.updatingChunks.visibleMap.forEach { (_, holder) ->
                @Suppress("UNNECESSARY_SAFE_CALL") // this may be null
                holder.fullChunkNow?.let { resolveChunk(it, bukkit) }
            }
        }

        private fun resolveChunk(chunk: LevelChunk, world: World) {
            val bukkit = chunk.bukkitChunk
            resolveTag(OBJECT_TYPE_CHUNK, tagOf(bukkit.persistentDataContainer)) { entity ->
                entity.addComponent(hostedByChunk(bukkit))
            }

            chunk.blockEntities.forEach { (pos, block) -> resolveBlock(block, pos, world) }
        }

        private fun resolveMob(mob: Entity) {
            val bukkit = lazy { mob.bukkitEntity }
            resolveTag(OBJECT_TYPE_ENTITY, tagOf(mob.bukkitEntity.persistentDataContainer)) { entity ->
                populate(entity, bukkit.value)
            }

            when (mob) {
                is ItemEntity -> resolveItem(mob.item)
                is ItemFrame -> resolveItem(mob.item)
                is Player -> mob.inventory.contents.forEach { resolveItem(it) }
                is LivingEntity -> mob.armorSlots.forEach { resolveItem(it) }
            }
        }

        private fun resolveBlock(block: BlockEntity, pos: BlockPos, world: World) {
            resolveTag(OBJECT_TYPE_BLOCK, tagOf(block.persistentDataContainer)) { entity ->
                val backing = lazy { world.getBlockAt(pos.x, pos.y, pos.z) }
                val state = lazy { backing.value.getState(false) }
                entity.addComponent(object : HostedByBlock {
                    override val block get() = backing.value
                    override val state get() = state.value
                })
            }

            when (block) {
                is BaseContainerBlockEntity -> block.contents.forEach { resolveItem(it) }
            }
        }

        private fun CompoundTag.compound(key: String) = tags[key] as? CompoundTag

        private fun CompoundTag.list(key: String) = tags[key] as? ListTag

        private fun resolveItemTagInternal(
            tagMeta: CompoundTag,
            getStack: () -> ItemStack,
            onDirtied: (BukkitStack) -> Unit
        ) {
            val bukkitStack = lazy { getStack().bukkitStack }
            val meta = lazy { bukkitStack.value.itemMeta }
            var dirty = false

            resolveTag(OBJECT_TYPE_ITEM, tagMeta.compound("PublicBukkitValues")?.compound(entityKey)) { entity ->
                entity.addComponent(object : HostedByItem {
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
                    resolveItemTag(tagItem) { dirtied ->
                        (dirtied as CraftItemStack).handle.save(tagItem)
                    }
                }
            }

            if (dirty) {
                bukkitStack.value.itemMeta = meta.value
                onDirtied(bukkitStack.value)
            }
        }

        private fun resolveItemTag(tag: CompoundTag, onDirtied: (BukkitStack) -> Unit) {
            tag.compound("tag")?.let { tagMeta ->
                val stack = lazy { ItemStack.of(tag) }
                resolveItemTagInternal(tagMeta, { stack.value }, onDirtied)
            }
        }

        private fun resolveItem(item: ItemStack) {
            val tag = item.tag
            if (item.item === Items.AIR || tag == null) return
            resolveItemTagInternal(tag, { item }, {})
        }
    }
}
