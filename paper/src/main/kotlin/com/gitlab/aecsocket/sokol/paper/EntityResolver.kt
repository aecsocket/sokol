package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.sokol.core.SokolEntityAccess
import com.gitlab.aecsocket.sokol.paper.component.*
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.server.ChunkSystem
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
import org.bukkit.craftbukkit.v1_19_R1.CraftEquipmentSlot
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

            ChunkSystem.getVisibleChunkHolders(level).forEach { holder ->
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
            val bukkit = mob.bukkitEntity
            resolveTag(OBJECT_TYPE_ENTITY, tagOf(bukkit.persistentDataContainer)) { entity ->
                populate(entity, bukkit)
            }

            when (mob) {
                is ItemEntity -> resolveItem(mob.item, ItemHolder.byMob(bukkit))
                is ItemFrame -> resolveItem(mob.item, ItemHolder.byMob(bukkit))
                is Player -> {
                    val player = bukkit as org.bukkit.entity.Player
                    resolveItem(mob.inventoryMenu.carried, ItemHolder.inCursor(player))
                    mob.inventory.contents.forEachIndexed { idx, item ->
                        resolveItem(item, ItemHolder.byPlayer(player, idx))
                    }
                }
                is LivingEntity -> {
                    EquipmentSlot.values().forEach { slot ->
                        val item = mob.getItemBySlot(slot)
                        resolveItem(item, ItemHolder.inEquipment(mob.bukkitLivingEntity, CraftEquipmentSlot.getSlot(slot)))
                    }
                }
            }
        }

        private fun resolveBlock(block: BlockEntity, pos: BlockPos, world: World) {
            val bukkit = world.getBlockAt(pos.x, pos.y, pos.z)
            val state = lazy { bukkit.getState(false) }
            resolveTag(OBJECT_TYPE_BLOCK, tagOf(block.persistentDataContainer)) { entity ->
                entity.addComponent(object : HostedByBlock {
                    override val block get() = bukkit
                    override val state get() = state.value
                })
            }

            when (block) {
                is BaseContainerBlockEntity -> {
                    val containerState = state.value as Container
                    val inventory = containerState.inventory
                    block.contents.forEachIndexed { idx, item ->
                        resolveItem(item, ItemHolder.byBlock(bukkit, inventory, idx))
                    }
                }
            }
        }

        private fun CompoundTag.compound(key: String) = tags[key] as? CompoundTag

        private fun CompoundTag.list(key: String) = tags[key] as? ListTag

        private fun resolveItemTagInternal(
            tagMeta: CompoundTag,
            holder: ItemHolder,
            getStack: () -> ItemStack,
            onDirtied: (BukkitStack) -> Unit
        ) {
            val bukkitStack = lazy { getStack().bukkitStack }
            val meta = lazy { bukkitStack.value.itemMeta }
            var dirty = false

            val tagEntity = tagMeta.compound("PublicBukkitValues")?.compound(entityKey)
            resolveTag(OBJECT_TYPE_ITEM, tagEntity) { entity ->
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
                entity.addComponent(holder)
            }

            tagMeta.compound("BlockEntityTag")?.list("Items")?.forEach { tagItem ->
                (tagItem as? CompoundTag)?.let {
                    val slot = tagItem.getByte("Slot").toInt()
                    resolveItemTag(tagItem, ItemHolder.byContainerItem(holder, slot)) { dirtied ->
                        (dirtied as CraftItemStack).handle.save(tagItem)
                    }
                }
            }

            if (dirty) {
                val iStack = bukkitStack.value
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

        private fun resolveItemTag(tag: CompoundTag, holder: ItemHolder, onDirtied: (BukkitStack) -> Unit) {
            tag.compound("tag")?.let { tagMeta ->
                val stack = lazy { ItemStack.of(tag) }
                resolveItemTagInternal(tagMeta, holder, { stack.value }, onDirtied)
            }
        }

        private fun resolveItem(item: ItemStack, holder: ItemHolder) {
            val tag = item.tag
            if (item.item === Items.AIR || tag == null) return
            resolveItemTagInternal(tag, holder, { item }) {}
        }
    }
}
