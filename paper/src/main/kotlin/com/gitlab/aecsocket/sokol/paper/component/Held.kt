package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.alexandria
import com.gitlab.aecsocket.craftbullet.core.transform
import com.gitlab.aecsocket.craftbullet.paper.CraftBulletAPI
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.extension.bullet
import com.gitlab.aecsocket.sokol.paper.*
import com.jme3.bullet.RotationOrder
import com.jme3.bullet.collision.shapes.EmptyShape
import com.jme3.bullet.joints.New6Dof
import com.jme3.bullet.joints.motors.MotorParam
import com.jme3.bullet.objects.PhysicsRigidBody
import com.jme3.math.Matrix3f
import com.jme3.math.Vector3f

data class Held(val hold: EntityHolding.Hold) : SokolComponent {
    companion object {
        fun init(ctx: Sokol.InitContext) {
            ctx.transientComponent<Held>()
            ctx.system { HeldColliderSystem(it) }
            ctx.system { HeldMobSystem(ctx.sokol, it) }
        }
    }

    data class Joint(
        val joint: New6Dof,
        val holdTarget: PhysicsRigidBody
    )

    override val componentType get() = Held::class

    var joint: Joint? = null
}

@All(Held::class, ColliderInstance::class)
@After(ColliderInstanceTarget::class)
class HeldColliderSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mHeld = ids.mapper<Held>()
    private val mColliderInstance = ids.mapper<ColliderInstance>()

    private fun updateBody(entity: SokolEntity, held: Held, isHeld: Boolean) {
        val (physObj, physSpace) = mColliderInstance.get(entity)
        val body = physObj.body
        if (body !is PhysicsRigidBody) return

        body.activate()

        if (isHeld) {
            if (held.joint != null) return

            val holdTarget = PhysicsRigidBody(EmptyShape(false))
            holdTarget.isKinematic = true
            // assigned later in PrePhysicsStep, through hold.nextTransform
            // but if hold.frozen, that isn't ran, so we do an initial transform set here
            holdTarget.transform = held.hold.nextTransform.bullet()
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
            held.joint = Held.Joint(joint, holdTarget)
        } else {
            // so if you don't make the body dynamic, literally none of the below code works
            // ok!
            body.isKinematic = false
            held.joint?.let { joint ->
                joint.joint.destroy()
                physSpace.removeJoint(joint.joint)
                physSpace.removeCollisionObject(joint.holdTarget)
                held.joint = null
            }
        }
    }

    @Subscribe
    fun on(event: ColliderSystem.CreatePhysics, entity: SokolEntity) {
        updateBody(entity, mHeld.get(entity), true)
    }

    @Subscribe
    fun on(event: EntityHolding.ChangeHoldState, entity: SokolEntity) {
        val held = mHeld.get(entity)
        // in executePhysics, `held` will already be removed
        // and we need its `joint` field to remove the joint from the phys space
        CraftBulletAPI.executePhysics {
            updateBody(entity, held, event.held)
        }
    }

    @Subscribe
    fun on(event: ColliderSystem.PrePhysicsStep, entity: SokolEntity) {
        val held = mHeld.get(entity)
        val hold = held.hold
        val (physObj) = mColliderInstance.get(entity)
        val body = physObj.body as? PhysicsRigidBody ?: return
        if (hold.frozen) return

        body.activate(true)
        (if (body.isKinematic) body else held.joint?.holdTarget)?.transform = hold.nextTransform.bullet()
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
        val oldEntity = sokol.resolver.mobTrackedBy(mob) ?: return
        val (oldHold) = mHeld.getOr(oldEntity) ?: return
        val hold = sokol.holding.start(oldHold.player.alexandria, entity, oldHold.operation, oldHold.nextTransform)
        hold.frozen = oldHold.frozen
    }

    @Subscribe
    fun on(event: MobEvent.RemoveFromWorld, entity: SokolEntity) {
        mHeld.getOr(entity)?.hold?.let { sokol.holding.stop(it) }
    }
}
