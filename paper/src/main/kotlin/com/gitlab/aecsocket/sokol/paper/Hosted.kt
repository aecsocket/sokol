package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.sokol.core.SokolComponent
import org.bukkit.Chunk
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

interface HostedByWorld : SokolComponent {
    override val componentType get() = HostedByWorld::class.java

    val world: World
}

interface HostedByChunk : SokolComponent {
    override val componentType get() = HostedByChunk::class.java

    val chunk: Chunk
}

interface HostedByMob : SokolComponent {
    override val componentType get() = HostedByMob::class.java

    val mob: Entity
}

interface HostedByBlock : SokolComponent {
    override val componentType get() = HostedByBlock::class.java

    val block: Block
    val state: BlockState
}

interface HostedByItem : SokolComponent {
    override val componentType get() = HostedByItem::class.java

    val stack: ItemStack

    fun <R> readMeta(action: (ItemMeta) -> R): R

    fun writeMeta(action: (ItemMeta) -> Unit)
}

fun hostedByWorld(world: World) = object : HostedByWorld {
    override val world get() = world
}

fun hostedByChunk(chunk: Chunk) = object : HostedByChunk {
    override val chunk get() = chunk
}

fun hostedByEntity(entity: Entity) = object : HostedByMob {
    override val mob get() = entity
}

fun hostedByItem(stack: ItemStack, meta: ItemMeta) = object : HostedByItem {
    override val stack get() = stack

    override fun <R> readMeta(action: (ItemMeta) -> R): R {
        return action(meta)
    }

    override fun writeMeta(action: (ItemMeta) -> Unit) {
        action(meta)
    }
}
