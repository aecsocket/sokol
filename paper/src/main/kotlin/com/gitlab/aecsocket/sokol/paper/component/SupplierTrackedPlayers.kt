package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.sokol.core.*
import org.bukkit.entity.Player

interface SupplierTrackedPlayers : SokolComponent {
    override val componentType get() = SupplierTrackedPlayers::class

    val trackedPlayers: () -> Set<Player>
}

object SupplierTrackedPlayersTarget : SokolSystem

@All(SupplierTrackedPlayers::class, Composite::class)
@Before(SupplierTrackedPlayersTarget::class)
class SupplierTrackedPlayersBuildSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mSupplierTrackedPlayers = mappers.componentMapper<SupplierTrackedPlayers>()
    private val mComposite = mappers.componentMapper<Composite>()

    @Subscribe
    fun on(event: Compose, entity: SokolEntity) {
        val trackedPlayersSupplier = mSupplierTrackedPlayers.get(entity)

        mComposite.forEachChild(entity) { (_, child) ->
            mSupplierTrackedPlayers.set(child, trackedPlayersSupplier)
            child.call(Compose)
        }
    }

    @Subscribe
    fun on(event: SokolEvent.Populate, entity: SokolEntity) {
        entity.call(Compose)
    }

    object Compose : SokolEvent
}
