package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.craftbullet.core.transform
import com.gitlab.aecsocket.craftbullet.paper.CraftBullet
import com.gitlab.aecsocket.craftbullet.paper.CraftBulletAPI
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.extension.bullet
import com.gitlab.aecsocket.sokol.core.extension.collisionOf
import com.gitlab.aecsocket.sokol.paper.*
import com.jme3.bullet.objects.PhysicsRigidBody
import org.bukkit.entity.Player

data class Held(val hold: EntityHolding.Hold) : SokolComponent {
    override val componentType get() = Held::class
}

@All(Held::class)
@After(PositionTarget::class)
class HeldSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mHeld = ids.mapper<Held>()

    @Subscribe
    fun on(event: UpdateEvent, entity: SokolEntity) {
        val (hold) = mHeld.get(entity)
        val player = hold.player

        if (hold.drawSlotShapes) {
            entity.call(HeldEntitySlotSystem.DrawShapes(player))
        }
    }
}

@All(EntitySlot::class, PositionRead::class)
class HeldEntitySlotSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mEntitySlot = ids.mapper<EntitySlot>()
    private val mPositionRead = ids.mapper<PositionRead>()

    data class DrawShapes(
        val player: Player
    ) : SokolEvent

    @Subscribe
    fun on(event: DrawShapes, entity: SokolEntity) {
        val entitySlot = mEntitySlot.get(entity)
        val positionRead = mPositionRead.get(entity)
        val player = event.player

        val transform = positionRead.transform.bullet()
        val drawable = CraftBulletAPI.drawableOf(player, CraftBullet.DrawType.SHAPE)
        CraftBulletAPI.drawPointsShape(collisionOf(entitySlot.shape)).forEach { point ->
            drawable.draw(transform.transform(point))
        }
    }
}

@All(ColliderInstance::class)
class HeldColliderSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mColliderInstance = ids.mapper<ColliderInstance>()

    data class ChangeBodyState(
        val held: Boolean
    ) : SokolEvent

    @Subscribe
    fun on(event: ChangeBodyState, entity: SokolEntity) {
        val (physObj) = mColliderInstance.get(entity)
        val body = physObj.body
        if (body !is PhysicsRigidBody) return

        val held = event.held
        body.activate()
        //body.isKinematic = held
        //body.isContactResponse = !held
    }

    @Subscribe
    fun on(event: EntityHolding.ChangeHoldState, entity: SokolEntity) {
        entity.call(ChangeBodyState(event.held))
    }
}

@All(IsMob::class)
class HeldMobSystem(
    private val sokol: Sokol,
    ids: ComponentIdAccess
) : SokolSystem {
    private val mIsMob = ids.mapper<IsMob>()
    private val mHeld = ids.mapper<Held>()

    @Subscribe
    fun on(event: ReloadEvent, entity: SokolEntity) {
        val mob = mIsMob.get(entity).mob
        val oldHeld = sokol.resolver.mobTrackedBy(mob)?.let { mHeld.getOr(it) } ?: return
        oldHeld.hold.entity = entity
        mHeld.set(entity, oldHeld)
    }

    @Subscribe
    fun on(event: MobEvent.RemoveFromWorld, entity: SokolEntity) {
        mHeld.getOr(entity)?.hold?.let { sokol.holding.stop(it) }
    }
}
