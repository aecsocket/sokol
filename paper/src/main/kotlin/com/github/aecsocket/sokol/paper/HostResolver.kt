package com.github.aecsocket.sokol.paper

import com.github.aecsocket.alexandria.core.LogLevel
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.NumericTag
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.chunk.LevelChunk
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.craftbukkit.v1_18_R2.CraftServer
import org.bukkit.craftbukkit.v1_18_R2.CraftWorld
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack
import org.bukkit.craftbukkit.v1_18_R2.persistence.CraftPersistentDataContainer
import org.bukkit.entity.Entity
import org.bukkit.entity.Item
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.persistence.PersistentDataHolder
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import java.util.*

enum class HostType {
    WORLD,
    CHUNK,
    ENTITY,
    STACK,
    BLOCK
}

interface HostsResolved {
    val possible: Int
    val marked: Int
}

class HostResolver(
    private val plugin: Sokol,
    val callback: (PaperTreeState, PaperNodeHost) -> Unit,
    var settings: Settings = Settings(),
) {
    @ConfigSerializable
    data class Settings(
        val enabled: Boolean = true,
        val containerItems: Boolean = true,
        val containerBlocks: Boolean = true,
    )

    internal fun load() {
        settings = plugin.settings.hostResolution
    }

    private data class HostsResolvedImpl(
        override var possible: Int = 0,
        override var marked: Int = 0
    ) : HostsResolved

    fun resolve(): Map<HostType, HostsResolved> = Operation().apply { resolve() }.resolved

    private inner class Operation {
        val resolved = EnumMap<HostType, HostsResolvedImpl>(HostType::class.java).apply {
            HostType.values().forEach { put(it, HostsResolvedImpl()) }
        }

        fun resolve() {
            if (!settings.enabled) return

            Bukkit.getServer().worlds.forEach { world ->
                forWorld(world)
            }

            (Bukkit.getServer() as CraftServer).server.playerList.players.forEach { player ->
                val bukkit = player.bukkitEntity
                forStack(player.containerMenu.carried, StackHolder.byCursor(PaperNodeHost.OfEntity(bukkit), bukkit))
            }
        }

        private fun runDataCallback(pdh: PersistentDataHolder, type: HostType, getHost: () -> PaperNodeHost) {
            val resolved = resolved[type]!!
            resolved.possible++
            runCallback((pdh.persistentDataContainer as CraftPersistentDataContainer).raw, resolved, getHost)
        }

        private fun runCallback(
            tag: MutableMap<String, Tag>,
            resolved: HostsResolvedImpl,
            getHost: () -> PaperNodeHost
        ): Boolean {
            tag[BUKKIT_PDC]?.let { tagPdc ->
                if (tagPdc is CompoundTag && tagPdc.tags[plugin.persistence.sTick]?.let {
                        it is NumericTag && it.asByte != (0).toByte()
                } == true) {
                    val host = getHost()
                    tagPdc[plugin.persistence.sNode]?.let { tagNodeNms ->
                        val tagNode = PaperCompoundTag(tagNodeNms as CompoundTag)
                        return plugin.persistence.nodeOf(tagNode)?.let { node ->
                            resolved.marked++
                            val state = paperStateOf(node)
                            callback(state, host)
                            plugin.persistence.stateInto(state, tagNode)
                            true
                        } ?: false
                    } ?: run {
                        plugin.log.line(LogLevel.Warning) { "Host $host was marked as ticking but is not node - removed tick key" }
                        tagPdc.remove(plugin.persistence.sTick)
                    }
                }
            }
            return false
        }

        private fun forWorld(world: World) {
            runDataCallback(world, HostType.WORLD) { PaperNodeHost.OfWorld(world) }

            val level = (world as CraftWorld).handle
            level.chunkSource.chunkMap.updatingChunks.visibleMap.forEach { (_, holder) ->
                @Suppress("UNNECESSARY_SAFE_CALL") // this may be null
                holder.fullChunkNow?.let { chunk ->
                    forChunk(world, level, chunk)
                }
            }
        }

        private fun forChunk(world: World, level: ServerLevel, chunk: LevelChunk) {
            val bukkit = chunk.bukkitChunk
            runDataCallback(bukkit, HostType.CHUNK) { PaperNodeHost.OfChunk(bukkit) }

            level.getChunkEntities(chunk.locX, chunk.locZ).forEach { forEntity(it) }
            chunk.blockEntities.forEach { (pos, block) -> forBlock(world, pos, block) }
        }

        private fun forEntity(entity: Entity) {
            val host by lazy { PaperNodeHost.OfEntity(entity) }
            runDataCallback(entity, HostType.ENTITY) { host }

            // *could* optimize this by using nms types?
            when (entity) {
                is Player -> {
                    entity.inventory.forEachIndexed { idx, stack ->
                        stack?.let {
                            forBukkitStack(stack, StackHolder.byPlayer(host, entity, idx))
                        }
                    }
                }
                is LivingEntity -> {
                    entity.equipment?.let { equipment ->
                        EquipmentSlot.values().forEach { slot ->
                            forBukkitStack(
                                equipment.getItem(slot),
                                StackHolder.byEquipment(host, entity, slot)
                            )
                        }
                    }
                }
                is Item -> forBukkitStack(entity.itemStack, StackHolder.byItemEntity(host, entity))
                is ItemFrame -> forBukkitStack(entity.item, StackHolder.byItemFrame(host, entity))
            }
        }

        private fun forBlock(world: World, pos: BlockPos, block: BlockEntity) {
            val resolved = resolved[HostType.BLOCK]!!
            resolved.possible++

            val host by lazy { PaperNodeHost.OfBlock(
                world.getBlockAt(pos.x, pos.y, pos.z).getState(false)
            ) }
            if (settings.containerBlocks && block is BaseContainerBlockEntity) {
                block.contents.forEachIndexed { _, stack ->
                    forStack(stack, StackHolder.byBlock(host))
                }
            }

            runCallback(block.persistentDataContainer.raw, resolved) { host }
        }

        private fun forBukkitStack(stack: org.bukkit.inventory.ItemStack, holder: StackHolder) {
            if (!stack.hasItemMeta()) return
            val craft = if (stack is CraftItemStack) stack else CraftItemStack.asCraftCopy(stack)
            craft.handle?.let {
                forStack(it, holder)
            }
        }

        private fun forStack(stack: ItemStack, holder: StackHolder) {
            if (stack.isEmpty)
                return
            val resolved = resolved[HostType.STACK]!!
            resolved.possible++

            stack.tag?.let { tag ->
                val bukkit by lazy { stack.bukkitStack }
                val meta by lazy { bukkit.itemMeta }
                var dirty = false
                val host by lazy { PaperNodeHost.OfWritableStack(
                    holder,
                    { bukkit },
                    { meta },
                    { dirty = true }
                ) }

                if (settings.containerItems) {
                    operator fun Tag.get(key: String) = if (this is CompoundTag) this.tags[key] else null

                    tag.tags["BlockEntityTag"]?.let { tagBlock ->
                        // if it's an item that holds other items, like a shulker box
                        // since shulker boxes can't hold other shulker boxes, this should get all items
                        tagBlock["Items"]?.let { tagItems ->
                            if (tagItems is ListTag) tagItems.forEach { tagItem ->
                                resolved.possible++
                                if (tagItem is CompoundTag) tagItem.tags["tag"]?.let { tagMeta ->
                                    if (tagMeta is CompoundTag) {
                                        val childBukkit by lazy { ItemStack.of(tagItem).bukkitStack }
                                        val childMeta by lazy { childBukkit.itemMeta }
                                        var childDirty = false
                                        val childHost by lazy {
                                            PaperNodeHost.OfWritableStack(
                                            StackHolder.byShulkerBox(host, tagItem.getByte("Slot").toInt()),
                                            { childBukkit },
                                            { childMeta },
                                            { childDirty = true }
                                        ) }
                                        if (runCallback(tagMeta.tags, resolved) { childHost }) {
                                            // we have modified the tagMeta (possibly)
                                            if (childDirty) {
                                                // meta has been dirtied; set it back into the stack
                                                childBukkit.itemMeta = childMeta
                                            }
                                            // save directly into our parent's BlockEntityTag.Items[idx]
                                            (childBukkit as CraftItemStack).handle.save(tagItem)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                runCallback(tag.tags, resolved) { host }

                // note: this may be dirtied even if the runCallback didn't run:
                //  · if this is a shulker box
                //  · and it has some items inside which are nodes
                //  · and those nodes dirtied this shulker box's meta
                if (dirty) {
                    bukkit.itemMeta = meta
                }
            }
        }
    }
}
