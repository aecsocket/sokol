package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.sokol.core.ComponentType
import com.gitlab.aecsocket.sokol.core.SokolComponent
import org.bukkit.Chunk
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

interface HostedByWorld : SokolComponent {
    override val type get() = HostedByWorld

    val world: World

    companion object : ComponentType<HostedByWorld>
}

interface HostedByChunk : SokolComponent {
    override val type get() = HostedByChunk

    val chunk: Chunk
    companion object : ComponentType<HostedByChunk>
}

interface HostedByEntity : SokolComponent {
    override val type get() = HostedByEntity

    val entity: Entity

    companion object : ComponentType<HostedByEntity>
}

interface HostedByBlock : SokolComponent {
    override val type get() = HostedByBlock

    val block: Block
    val state: BlockState

    companion object : ComponentType<HostedByBlock>
}

interface HostedByItem : SokolComponent {
    override val type get() = HostedByItem

    val stack: ItemStack

    fun <R> readMeta(action: (ItemMeta) -> R): R

    fun writeMeta(action: (ItemMeta) -> Unit)

    companion object : ComponentType<HostedByItem>
}

fun hostedByWorld(world: World) = object : HostedByWorld {
    override val world get() = world
}

fun hostedByChunk(chunk: Chunk) = object : HostedByChunk {
    override val chunk get() = chunk
}

fun hostedByEntity(entity: Entity) = object : HostedByEntity {
    override val entity get() = entity
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
