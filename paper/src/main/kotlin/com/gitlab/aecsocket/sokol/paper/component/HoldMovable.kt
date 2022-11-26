package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.with
import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.core.physics.Vector3
import com.gitlab.aecsocket.alexandria.paper.alexandria
import com.gitlab.aecsocket.alexandria.paper.extension.*
import com.gitlab.aecsocket.craftbullet.core.*
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.extension.bullet
import com.gitlab.aecsocket.sokol.paper.*
import com.jme3.bullet.RotationOrder
import com.jme3.bullet.joints.New6Dof
import com.jme3.bullet.joints.PhysicsJoint
import com.jme3.bullet.joints.motors.MotorParam
import com.jme3.bullet.objects.PhysicsRigidBody
import com.jme3.math.Matrix3f
import com.jme3.math.Vector3f
import com.simsilica.mathd.Vec3d
import org.bukkit.Particle
import org.spongepowered.configurate.objectmapping.ConfigSerializable

data class HoldMovable(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("hold_movable")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = HoldMovable::class
    override val key get() = Key

    var holdJoint: New6Dof? = null

    @ConfigSerializable
    data class Profile(
        val holdTransform: Transform = Transform.Identity,
        val holdDistance: Double = 0.0
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
        val StartMove = HoldMovable.Key.with("start")
    }

    private val mInputCallbacks = ids.mapper<InputCallbacks>()
    private val mHeld = ids.mapper<Held>()
    private val mPositionRead = ids.mapper<PositionRead>()

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        val inputCallbacks = mInputCallbacks.get(entity)
        val positionRead = mPositionRead.get(entity)

        inputCallbacks.callback(StartMove) { player, cancel ->
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

    @Subscribe
    fun on(event: EntityHolding.ChangeHoldState, entity: SokolEntity) {
        val holdMovable = mHoldMovable.get(entity)
        val (hold) = mHeld.get(entity)
        val (physObj, physSpace) = mColliderInstance.get(entity)
        val body = physObj.body as? PhysicsRigidBody ?: return

        holdMovable.holdJoint?.let {
            physSpace.removeJoint(it)
            holdMovable.holdJoint = null
        }

        if (event.held) {
            /*val operation = hold.operation as? MoveHoldOperation ?: return

            val transform = operation.nextTransform
            // todo mat rot
            val joint = New6Dof(body,
                Vector3f.ZERO, (transform.translation + Vector3(0.0, 0.5, 0.0)).bullet().sp(),
                Matrix3f.IDENTITY, Matrix3f.IDENTITY,
                RotationOrder.XYZ
            )

            repeat(6) {
                joint.set(MotorParam.LowerLimit, it, 0f)
                joint.set(MotorParam.UpperLimit, it, 0f)
            }

            physSpace.addJoint(joint)
            holdMovable.holdJoint = joint*/
        }
    }

    @Subscribe
    fun on(event: ColliderSystem.PostPhysicsStep, entity: SokolEntity) {
        val holdMovable = mHoldMovable.get(entity)
        val (hold) = mHeld.get(entity)
        val (physObj) = mColliderInstance.get(entity)
        val body = physObj.body
        val player = hold.player

        val operation = hold.operation as? MoveHoldOperation ?: return
        if (hold.frozen) return

        body.transform = operation.nextTransform.bullet()
        if (body is PhysicsRigidBody) {
            body.linearVelocity = Vec3d.ZERO
            body.angularVelocity = Vec3d.ZERO
        }
        // A = world, B = body
        body.activate(true)
        //holdMovable.holdJoint?.setPivotInA(operation.nextTransform.translation.bullet().sp())
        //player.spawnParticle(Particle.END_ROD, operation.nextTransform.translation.location(player.world), 0)

        val from = player.eyeLocation
        val direction = from.direction.alexandria()

        operation.nextTransform = Transform(
            (from + direction * holdMovable.profile.holdDistance).position(),
            from.rotation()
        ) + holdMovable.profile.holdTransform
    }
}
