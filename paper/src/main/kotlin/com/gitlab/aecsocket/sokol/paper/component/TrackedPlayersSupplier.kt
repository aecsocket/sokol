package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.sokol.core.*
import org.bukkit.entity.Player

interface TrackedPlayersSupplier : SokolComponent {
    override val componentType get() = TrackedPlayersSupplier::class

    val trackedPlayers: () -> Set<Player>
}

@All(TrackedPlayersSupplier::class, Composite::class)
class TrackedPlayersSupplierComposeSystem(engine: SokolEngine) : SokolSystem {
    private val mTrackedPlayersSupplier = engine.componentMapper<TrackedPlayersSupplier>()
    private val mComposite = engine.componentMapper<Composite>()

    @Subscribe
    fun on(event: Compose, entity: SokolEntity) {
        val trackedPlayersSupplier = mTrackedPlayersSupplier.map(entity)

        mComposite.forEachChild(entity) { (_, child) ->
            child.components.set(trackedPlayersSupplier)
            child.call(Compose)
        }
    }

    @Subscribe
    fun on(event: SokolEvent.Populate, entity: SokolEntity) {
        entity.call(Compose)
    }

    object Compose : SokolEvent
}
