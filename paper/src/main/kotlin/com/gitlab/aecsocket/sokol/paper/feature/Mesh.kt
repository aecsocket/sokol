package com.gitlab.aecsocket.sokol.paper.feature

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.ByEntityEvent
import com.gitlab.aecsocket.sokol.paper.HostedByEntity
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import org.bukkit.entity.Player
import org.spongepowered.configurate.ConfigurationNode

private const val MESH = "mesh"

class MeshComponent : SokolComponentType {
    override val key get() = MeshComponent.key

    override fun deserialize(node: ConfigurationNode) = Component()

    override fun deserialize(tag: CompoundNBTTag) = Component()

    inner class Component : SokolComponent.Persistent {
        override val key get() = ColliderComponent.key

        override fun serialize(node: ConfigurationNode) {}

        override fun serialize(tag: CompoundNBTTag.Mutable) {}
    }

    companion object : ComponentKey<Component> {
        override val key = SokolAPI.key(MESH)
    }
}

private val filter = entityFilterOf(setOf(
    HostedByEntity.key,
    ColliderComponent.key,
    MeshComponent.key,
))

class MeshSystem(
    private val sokol: Sokol
) : SokolSystem {
    override fun handle(entities: EntityAccessor, event: SokolEvent) {
        when (event) {
            is ByEntityEvent.Shown -> {
                val player = event.backing.player as Player
                event.backing.isCancelled = true

                // TODO actual logic

            }
            is ByEntityEvent.Hidden -> {
                val player = event.backing.player as Player
                event.backing.isCancelled = true


            }
        }
    }
}
