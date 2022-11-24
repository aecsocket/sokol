package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.EntityHolding
import com.gitlab.aecsocket.sokol.paper.MobEvent
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.UpdateEvent
import com.jme3.bullet.objects.PhysicsRigidBody

data class Held(val hold: EntityHolding.Hold) : SokolComponent {
    override val componentType get() = Held::class
}

@All(Held::class)
class HeldSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mHeld = ids.mapper<Held>()

    @Subscribe
    fun on(event: UpdateEvent, entity: SokolEntity) {
        val (hold) = mHeld.get(entity)
        val player = hold.player

        if (hold.drawSlotShapes) {
            // TODO
        }
    }
}

@All(Held::class, ColliderInstance::class)
class HeldColliderSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mColliderInstance = ids.mapper<ColliderInstance>()

    @Subscribe
    fun on(event: EntityHolding.ChangeHoldState, entity: SokolEntity) {
        val (physObj) = mColliderInstance.get(entity)
        val body = physObj.body
        if (body !is PhysicsRigidBody) return

        val held = event.held
        body.isKinematic = held
        body.isContactResponse = !held
    }
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
