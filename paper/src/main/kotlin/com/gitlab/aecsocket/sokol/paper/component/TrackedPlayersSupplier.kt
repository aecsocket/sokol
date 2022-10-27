package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.sokol.core.*
import org.bukkit.entity.Player

interface TrackedPlayersSupplier : SokolComponent {
    override val componentType get() = TrackedPlayersSupplier::class

    val trackedPlayers: () -> Set<Player>
}

@All(TrackedPlayersSupplier::class, Composite::class)
class TrackedPlayersSupplierComposeSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mTrackedPlayersSupplier = mappers.componentMapper<TrackedPlayersSupplier>()
    private val mComposite = mappers.componentMapper<Composite>()

    @Subscribe
    fun on(event: Compose, entity: SokolEntity) {
        val trackedPlayersSupplier = mTrackedPlayersSupplier.get(entity)

        mComposite.forEachChild(entity) { (_, child) ->
            mTrackedPlayersSupplier.set(child, trackedPlayersSupplier)
            child.call(Compose)
        }
    }

    @Subscribe
    fun on(event: SokolEvent.Populate, entity: SokolEntity) {
        entity.call(Compose)
    }

    object Compose : SokolEvent
}
