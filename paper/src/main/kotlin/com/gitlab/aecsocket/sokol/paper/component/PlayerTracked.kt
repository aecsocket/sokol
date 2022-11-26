package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.sokol.core.*
import org.bukkit.entity.Player

interface PlayerTracked : SokolComponent {
    override val componentType get() = PlayerTracked::class

    fun trackedPlayers(): Set<Player>
}

object PlayerTrackedTarget : SokolSystem

@All(IsChild::class)
@After(PlayerTrackedTarget::class)
class PlayerTrackedSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mIsChild = ids.mapper<IsChild>()
    private val mPlayerTracked = ids.mapper<PlayerTracked>()

    private fun construct(entity: SokolEntity) {
        val isChild = mIsChild.get(entity)

        mPlayerTracked.set(entity, mPlayerTracked.getOr(isChild.parent) ?: return)
    }

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        construct(entity)
    }

    @Subscribe
    fun on(event: Composite.Attach, entity: SokolEntity) {
        construct(entity)
    }
}
