package com.github.aecsocket.sokol.paper

import com.github.aecsocket.alexandria.core.vector.Polar3
import com.github.aecsocket.alexandria.core.vector.Vector3
import com.github.aecsocket.alexandria.paper.ServerElement
import com.github.aecsocket.alexandria.paper.StackHolder
import com.github.aecsocket.alexandria.paper.extension.polar
import com.github.aecsocket.alexandria.paper.extension.vector
import com.github.aecsocket.sokol.core.NodeHost
import org.bukkit.Chunk
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.TileState
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataContainer

interface PaperNodeHost : NodeHost {
    val pdc: PersistentDataContainer

    interface WithPosition : PaperNodeHost {
        val world: World
        val position: Vector3
    }

    interface WithDirection : WithPosition {
        val direction: Polar3
    }

    interface OfWorld : PaperNodeHost {
        val world: World

        override val pdc: PersistentDataContainer
            get() = world.persistentDataContainer
    }

    interface OfChunk : PaperNodeHost {
        val chunk: Chunk

        override val pdc: PersistentDataContainer
            get() = chunk.persistentDataContainer
    }

    interface OfEntity : PaperNodeHost, WithDirection {
        val entity: Entity

        override val pdc: PersistentDataContainer
            get() = entity.persistentDataContainer
        override val world: World
            get() = entity.world
        override val position: Vector3
            get() = entity.location.vector()
        override val direction: Polar3
            get() = entity.location.polar()
    }

    interface OfStack : PaperNodeHost {
        val stack: ItemStack
        val meta: ItemMeta

        override val pdc: PersistentDataContainer
            get() = meta.persistentDataContainer
    }

    interface OfBlock : PaperNodeHost, WithPosition {
        val block: Block
        val state: TileState

        override val pdc: PersistentDataContainer
            get() = state.persistentDataContainer
        override val world: World
            get() = block.world
        override val position: Vector3
            get() = block.location.vector()
    }

    abstract class OfElement(
        val element: ServerElement
    ) : PaperNodeHost {
        override fun toString() = "<$element>"
    }

    companion object {
        // todo better
        fun from(element: ServerElement): PaperNodeHost {
            return when (element) {
                is ServerElement.OfWorld -> object : OfElement(element), OfWorld {
                    override val world: World
                        get() = element.world
                }
                is ServerElement.OfChunk -> object : OfElement(element), OfChunk {
                    override val chunk: Chunk
                        get() = element.chunk
                }
                is ServerElement.OfEntity -> object : OfElement(element), OfEntity {
                    override val entity: Entity
                        get() = element.entity
                }
                is ServerElement.OfBlock -> {
                    // todo weird cast
                    object : OfElement(element), OfBlock {
                        override val block: Block
                            get() = element.block
                        override val state: TileState
                            get() = element.block.state as TileState // todo cache
                    }
                }
                else -> throw IllegalArgumentException("Invalid element type ${element::class}")
            }
        }

        fun fromStack(element: ServerElement.OfStack, meta: ItemMeta): PaperNodeHost {
            val stack = element.stack

            abstract class OfStackImpl : OfElement(element), OfStack {
                override val stack: ItemStack
                    get() = stack
                override val meta: ItemMeta
                    get() = meta
            }

            fun byStackHolder(holder: StackHolder): PaperNodeHost = when (holder) {
                is StackHolder.ByPlayerInventory -> object : OfStackImpl(), OfEntity {
                    override val entity: Player
                        get() = holder.entity
                    override val pdc: PersistentDataContainer
                        get() = super<OfStackImpl>.pdc
                }
                is StackHolder.ByEquipment -> object : OfStackImpl(), OfEntity {
                    override val entity: LivingEntity
                        get() = holder.entity
                    override val pdc: PersistentDataContainer
                        get() = super<OfStackImpl>.pdc
                }
                is StackHolder.ByEntity -> object : OfStackImpl(), OfEntity {
                    override val entity: Entity
                        get() = holder.entity
                    override val pdc: PersistentDataContainer
                        get() = super<OfStackImpl>.pdc
                }
                is StackHolder.ByBlock -> object : OfStackImpl(), OfBlock {
                    override val block: Block
                        get() = holder.block
                    override val state: TileState
                        get() = holder.block.state as TileState // todo cache this omg
                    override val pdc: PersistentDataContainer
                        get() = super<OfStackImpl>.pdc
                }
                is StackHolder.ByStack -> byStackHolder(holder.parent.holder)
            }

            return byStackHolder(element.holder)
        }
    }
}
