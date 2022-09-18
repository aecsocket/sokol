package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.ComponentKey
import com.gitlab.aecsocket.sokol.core.SokolComponent
import org.bukkit.Chunk
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

interface HostedByWorld : SokolComponent {
    override val key get() = HostedByWorld.key
    
    val world: World
    
    companion object : ComponentKey<HostedByWorld> {
        override val key = SokolAPI.key("hosted_by_world")
    }
}

interface HostedByChunk : SokolComponent {
    override val key get() = HostedByChunk.key

    val chunk: Chunk

    companion object : ComponentKey<HostedByChunk> {
        override val key = SokolAPI.key("hosted_by_chunk")
    }
}

interface HostedByEntity : SokolComponent {
    override val key get() = HostedByEntity.key

    val entity: Entity

    companion object : ComponentKey<HostedByEntity> {
        override val key = SokolAPI.key("hosted_by_entity")
    }
}

interface HostedByBlock : SokolComponent {
    override val key get() = HostedByBlock.key

    val state: BlockState
    val block: Block

    companion object : ComponentKey<HostedByBlock> {
        override val key = SokolAPI.key("hosted_by_block")
    }
}

interface HostedByItem : SokolComponent {
    override val key get() = HostedByItem.key

    val stack: ItemStack

    fun <R> readMeta(action: (ItemMeta) -> R): R

    fun writeMeta(action: (ItemMeta) -> Unit)

    companion object : ComponentKey<HostedByItem> {
        override val key = SokolAPI.key("hosted_by_item")
    }
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
