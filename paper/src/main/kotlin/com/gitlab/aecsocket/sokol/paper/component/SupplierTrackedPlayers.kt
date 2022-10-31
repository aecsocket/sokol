package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.sokol.core.*
import org.bukkit.entity.Player

interface SupplierTrackedPlayers : SokolComponent {
    override val componentType get() = SupplierTrackedPlayers::class

    val trackedPlayers: () -> Set<Player>
}

object SupplierTrackedPlayersTarget : SokolSystem

@Before(SupplierTrackedPlayersTarget::class)
class SupplierTrackedPlayersBuildSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mSupplierTrackedPlayers = mappers.componentMapper<SupplierTrackedPlayers>()

    @Subscribe
    fun on(event: CompositeSystem.AttachTo, entity: SokolEntity) {
        val parentSupplier = mSupplierTrackedPlayers.getOr(event.parent) ?: return
        mSupplierTrackedPlayers.set(entity, parentSupplier)
    }
}
