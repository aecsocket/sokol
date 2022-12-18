package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.with
import com.gitlab.aecsocket.alexandria.core.extension.clamp
import com.gitlab.aecsocket.alexandria.core.physics.*
import com.gitlab.aecsocket.alexandria.paper.alexandria
import com.gitlab.aecsocket.alexandria.paper.extension.*
import com.gitlab.aecsocket.craftbullet.core.*
import com.gitlab.aecsocket.craftbullet.paper.CraftBulletAPI
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.*
import com.jme3.bullet.objects.PhysicsRigidBody
import org.spongepowered.configurate.objectmapping.ConfigSerializable

data class HoldDetachable(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("hold_detachable")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = HoldDetachable::class
    override val key get() = Key

    var localTransform = Transform.Identity

    @ConfigSerializable
    data class Profile(
        val detachAxis: Vector3 = Vector3.Zero,
        val stopAt: Double = 0.0,
        val detachAt: Double = 0.0,
        val hasCollision: Boolean = true
    ) : SimpleComponentProfile {
        override val componentType get() = HoldDetachable::class

        val detachAxisNorm = detachAxis.normalized

        override fun createEmpty() = ComponentBlueprint { HoldDetachable(this) }
    }
}

class DetachHoldOperation : HoldOperation {
    override val canRelease get() = true
}

@All(HoldDetachable::class)
@Before(LocalTransformTarget::class)
class HoldDetachableLocalSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mHoldDetachable = ids.mapper<HoldDetachable>()
    private val mLocalTransform = ids.mapper<LocalTransform>()

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        val holdDetachable = mHoldDetachable.get(entity)
        mLocalTransform.combine(entity, holdDetachable.localTransform)
    }
}

@All(HoldDetachable::class, InputCallbacks::class, PositionRead::class)
@Before(InputCallbacksSystem::class)
@After(PositionAccessTarget::class)
class HoldDetachableCallbackSystem(
    private val sokol: Sokol,
    ids: ComponentIdAccess
) : SokolSystem {
    companion object {
        val Start = HoldDetachable.Key.with("start")
    }

    private val mInputCallbacks = ids.mapper<InputCallbacks>()
    private val mHeld = ids.mapper<Held>()
    private val mPositionRead = ids.mapper<PositionRead>()
    private val mIsChild = ids.mapper<IsChild>()

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        val inputCallbacks = mInputCallbacks.get(entity)
        val positionRead = mPositionRead.get(entity)

        inputCallbacks.callback(Start) { player, cancel ->
            if (mHeld.has(entity)) return@callback false // this entity is already held
            if (!mIsChild.has(entity)) return@callback false

            cancel()
            sokol.holding.start(player.alexandria, entity, DetachHoldOperation(), positionRead.transform)
            true
        }
    }
}

@All(HoldDetachable::class, Held::class, ColliderInstance::class, IsChild::class, PositionRead::class)
@After(ColliderInstanceTarget::class, PositionAccessTarget::class)
class HoldDetachableColliderSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mHoldDetachable = ids.mapper<HoldDetachable>()
    private val mHeld = ids.mapper<Held>()
    private val mColliderInstance = ids.mapper<ColliderInstance>()
    private val mIsChild = ids.mapper<IsChild>()
    private val mPositionRead = ids.mapper<PositionRead>()
    private val mLocalTransform = ids.mapper<LocalTransform>()

    private fun updateBody(entity: SokolEntity, isHeld: Boolean) {
        val holdDetachable = mHoldDetachable.get(entity)
        val (physObj, _, parentJoint) = mColliderInstance.get(entity)
        val body = physObj.body as? PhysicsRigidBody ?: return

        if (!holdDetachable.profile.hasCollision) {
            body.isContactResponse = !isHeld
        }

        parentJoint?.let { joint ->
            joint.isEnabled = !isHeld
        }
    }

    @Subscribe
    fun on(event: ColliderPhysicsSystem.CreatePhysics, entity: SokolEntity) {
        updateBody(entity, true)
    }

    @Subscribe
    fun on(event: EntityHolding.ChangeHoldState, entity: SokolEntity) {
        CraftBulletAPI.executePhysics {
            updateBody(entity, event.held)
        }
    }

    @Subscribe
    fun on(event: ColliderPhysicsSystem.PrePhysicsStep, entity: SokolEntity) {
        val holdDetachable = mHoldDetachable.get(entity)
        val (hold) = mHeld.get(entity)
        val (physObj) = mColliderInstance.get(entity)
        val parent = mIsChild.get(entity).parent
        val pPositionRead = mPositionRead.getOr(parent) ?: return
        val player = hold.player

        val operation = hold.operation as? DetachHoldOperation ?: return
        if (hold.frozen) return

        val pTransform = pPositionRead.transform
        val planeOrigin = pTransform.translation

        val eyeLocation = player.eyeLocation
        val from = eyeLocation.position()
        val direction = eyeLocation.direction.alexandria()
        val ray = Ray(from - planeOrigin, direction)

        val planeNormal = (from - planeOrigin).normalized
        val plane = PlaneShape(planeNormal)

        plane.testRay(ray)?.let { (tIn) ->
            val axis = holdDetachable.profile.detachAxisNorm
            val intersect = from + direction * tIn
            val distanceAlongAxis = (intersect - planeOrigin).dot(axis)

            if (distanceAlongAxis >= holdDetachable.profile.detachAt) {

            } else {
                val relative = Transform(axis * clamp(distanceAlongAxis, 0.0, holdDetachable.profile.stopAt))
                holdDetachable.localTransform = relative
                // todo how do we apply a local transform for the next tick?
                hold.nextTransform = pTransform * relative
            }
        }
    }
}
