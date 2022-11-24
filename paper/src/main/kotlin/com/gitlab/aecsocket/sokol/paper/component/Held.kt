package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.EntityHolding
import com.gitlab.aecsocket.sokol.paper.MobEvent
import com.gitlab.aecsocket.sokol.paper.Sokol

data class Held(val hold: EntityHolding.Hold) : SokolComponent {
    override val componentType get() = Held::class
}

@All(IsMob::class)
class HeldMobSystem(
    private val sokol: Sokol,
    ids: ComponentIdAccess
) : SokolSystem {
    private val mHeld = ids.mapper<Held>()
    private val mIsMob = ids.mapper<IsMob>()

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        val mob = mIsMob.get(entity).mob

        sokol.holding.holdOf(mob)?.let { hold ->
            hold.entity = entity
            mHeld.set(entity, Held(hold))
        }
    }

    @Subscribe
    fun on(event: MobEvent.RemoveFromWorld, entity: SokolEntity) {
        val (hold) = mHeld.getOr(entity) ?: return

        sokol.holding.stop(hold)
    }
}
