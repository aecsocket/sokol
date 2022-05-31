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
}
