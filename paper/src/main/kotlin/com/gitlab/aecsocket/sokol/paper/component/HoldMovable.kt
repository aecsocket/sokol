package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.with
import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.alexandria
import com.gitlab.aecsocket.alexandria.paper.extension.*
import com.gitlab.aecsocket.craftbullet.core.*
import com.gitlab.aecsocket.craftbullet.paper.CraftBulletAPI
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.extension.bullet
import com.gitlab.aecsocket.sokol.paper.*
import com.jme3.bullet.PhysicsSpace
import com.jme3.bullet.RotationOrder
import com.jme3.bullet.collision.shapes.EmptyShape
import com.jme3.bullet.joints.New6Dof
import com.jme3.bullet.joints.motors.MotorParam
import com.jme3.bullet.objects.PhysicsRigidBody
import com.jme3.math.Matrix3f
import com.jme3.math.Vector3f
import org.spongepowered.configurate.objectmapping.ConfigSerializable

data class HoldMovable(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("hold_movable")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = HoldMovable::class
    override val key get() = Key

    data class HoldJoint(
        val joint: New6Dof,
        val holdTarget: PhysicsRigidBody
    )

    var joint: HoldJoint? = null

    @ConfigSerializable
    data class Profile(
        val holdTransform: Transform = Transform.Identity,
        val holdDistance: Double = 0.0,
        val hasCollision: Boolean = true
    ) : SimpleComponentProfile {
        override val componentType get() = HoldMovable::class

        override fun createEmpty() = ComponentBlueprint { HoldMovable(this) }
    }
}

data class MoveHoldOperation(
    var nextTransform: Transform
) : HoldOperation {
    override var canRelease = false
}

@All(HoldMovable::class, InputCallbacks::class, PositionRead::class)
@Before(InputCallbacksSystem::class)
@After(PositionTarget::class)
class HoldMovableCallbackSystem(
    private val sokol: Sokol,
    ids: ComponentIdAccess
) : SokolSystem {
    companion object {
        val Start = HoldMovable.Key.with("start")
    }

    private val mInputCallbacks = ids.mapper<InputCallbacks>()
    private val mHeld = ids.mapper<Held>()
    private val mPositionRead = ids.mapper<PositionRead>()

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        val inputCallbacks = mInputCallbacks.get(entity)
        val positionRead = mPositionRead.get(entity)

        inputCallbacks.callback(Start) { player, cancel ->
            if (mHeld.has(entity)) // this entity is already held
                return@callback false

            cancel()
            sokol.holding.start(player.alexandria, entity, MoveHoldOperation(positionRead.transform))
            true
        }
    }
}

@All(HoldMovable::class, Held::class, ColliderInstance::class)
@After(ColliderInstanceTarget::class)
class HoldMovableColliderSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mHoldMovable = ids.mapper<HoldMovable>()
    private val mHeld = ids.mapper<Held>()
    private val mColliderInstance = ids.mapper<ColliderInstance>()

    private fun updateBody(entity: SokolEntity, held: Boolean) {
        val holdMovable = mHoldMovable.get(entity)
        val (physObj, physSpace) = mColliderInstance.get(entity)
        val body = physObj.body as? PhysicsRigidBody ?: return

        if (!holdMovable.profile.hasCollision) {
            body.isContactResponse = !held
        }

        if (held) {
            if (holdMovable.joint != null) return

            val holdTarget = PhysicsRigidBody(EmptyShape(false))
            holdTarget.isKinematic = true
            physSpace.addCollisionObject(holdTarget)

            val joint = New6Dof(
                body, holdTarget,
                Vector3f.ZERO, Vector3f.ZERO,
                Matrix3f.IDENTITY, Matrix3f.IDENTITY,
                RotationOrder.XYZ
            )

            repeat(6) {
                joint.set(MotorParam.LowerLimit, it, 0f)
                joint.set(MotorParam.UpperLimit, it, 0f)
            }

            physSpace.addJoint(joint)
            holdMovable.joint = HoldMovable.HoldJoint(joint, holdTarget)
        } else {
            // so if you don't make the body dynamic, literally none of the below code works
            // ok!
            body.isKinematic = false
            holdMovable.joint?.let { joint ->
                physSpace.removeCollisionObject(joint.holdTarget)
                physSpace.removeJoint(joint.joint)
                holdMovable.joint = null
            }
        }
    }

    @Subscribe
    fun on(event: ColliderSystem.CreatePhysics, entity: SokolEntity) {
        if (!mHeld.has(entity)) return
        updateBody(entity, true)
    }

    @Subscribe
    fun on(event: EntityHolding.ChangeHoldState, entity: SokolEntity) {
        CraftBulletAPI.executePhysics {
            updateBody(entity, event.held)
        }
    }

    @Subscribe
    fun on(event: ColliderSystem.PrePhysicsStep, entity: SokolEntity) {
        val holdMovable = mHoldMovable.get(entity)
        val (hold) = mHeld.get(entity)
        val (physObj) = mColliderInstance.get(entity)
        val body = physObj.body as? PhysicsRigidBody ?: return
        val player = hold.player

        val operation = hold.operation as? MoveHoldOperation ?: return
        if (hold.frozen) return

        body.activate(true)
        (if (body.isKinematic) body else holdMovable.joint?.holdTarget)?.transform = operation.nextTransform.bullet()

        val from = player.eyeLocation
        val direction = from.direction.alexandria()

        operation.nextTransform = Transform(
            (from + direction * holdMovable.profile.holdDistance).position(),
            from.rotation()
        ) + holdMovable.profile.holdTransform
    }
}
