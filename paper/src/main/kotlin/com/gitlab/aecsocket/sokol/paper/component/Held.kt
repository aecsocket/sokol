package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.Euler3
import com.gitlab.aecsocket.alexandria.core.extension.EulerOrder
import com.gitlab.aecsocket.alexandria.core.extension.quaternion
import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.core.physics.Vector3
import com.gitlab.aecsocket.alexandria.core.physics.quaternionFromTo
import com.gitlab.aecsocket.alexandria.core.physics.quaternionOfAxes
import com.gitlab.aecsocket.alexandria.paper.extension.alexandria
import com.gitlab.aecsocket.alexandria.paper.extension.plus
import com.gitlab.aecsocket.alexandria.paper.extension.position
import com.gitlab.aecsocket.alexandria.paper.extension.rotation
import com.gitlab.aecsocket.craftbullet.core.*
import com.gitlab.aecsocket.craftbullet.paper.rayTestFrom
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.extension.alexandria
import com.gitlab.aecsocket.sokol.paper.EntityHolding
import com.gitlab.aecsocket.sokol.paper.MobEvent
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.jme3.bullet.objects.PhysicsRigidBody
import kotlin.math.PI
import kotlin.math.abs

data class Held(val hold: EntityHolding.Hold) : SokolComponent {
    override val componentType get() = Held::class
}

@All(Held::class, PositionWrite::class, ColliderInstance::class)
@After(PositionTarget::class, ColliderInstanceTarget::class)
class HeldPositionSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mHeld = ids.mapper<Held>()
    private val mPositionWrite = ids.mapper<PositionWrite>()
    private val mColliderInstance = ids.mapper<ColliderInstance>()

    @Subscribe
    fun on(event: EntityHolding.ChangeHoldState, entity: SokolEntity) {
        val (physObj) = mColliderInstance.get(entity)
        val body = physObj.body

        if (body is PhysicsRigidBody) {
            body.isKinematic = event.held
        }
    }

    @Subscribe
    fun on(event: ColliderSystem.PrePhysicsStep, entity: SokolEntity) {
        val (hold) = mHeld.get(entity)
        val positionWrite = mPositionWrite.get(entity)
        val (physObj) = mColliderInstance.get(entity)
        val body = physObj.body
        val player = hold.player

        when (val operation = hold.operation) {
            is MoveHoldOperation -> {
                val settings = operation.settings
                val rayTest = player.rayTestFrom(settings.snapDistance.toFloat())
                    .firstOrNull {
                        val obj = it.collisionObject
                        obj !is TrackedPhysicsObject || obj.id != physObj.id
                    }

                val from = player.eyeLocation
                val direction = from.direction.alexandria()

                val transform: Transform
                if (rayTest == null) {
                    operation.state = if (settings.allowFreePlace) MoveHoldState.ALLOW else MoveHoldState.DISALLOW
                    transform = Transform(
                        (from + direction * settings.holdDistance).position(),
                        from.rotation()
                    )
                } else {
                    operation.state = MoveHoldState.ALLOW

                    // the hit normal is facing from the surface, to the player
                    // but when holding (non-snap) it's the opposite direction
                    // so we invert the normal here to face it in the right direction
                    val hitPos = from.position() + direction * (settings.snapDistance * rayTest.hitFraction)
                    val dir = -rayTest.hitNormal.alexandria()
                    val rotation = if (abs(dir.dot(Vector3.Up)) > 0.99) {
                        // `dir` and `up` are (close to) collinear
                        val yaw = radians(player.location.yaw).toDouble()
                        // `rotation` will be facing "away" from the player
                        quaternionFromTo(
                            Vector3.Forward, dir
                        ) * Euler3(z = (if (dir.y > 0.0) -yaw else yaw) + 2 * PI).quaternion(EulerOrder.XYZ)
                    } else {
                        val v1 = Vector3.Up.cross(dir).normalized
                        val v2 = dir.cross(v1).normalized
                        quaternionOfAxes(v1, v2, dir)
                    }

                    transform = Transform(hitPos, rotation) + settings.holdTransform
                }

                positionWrite.transform = transform
                /*if (body is PhysicsRigidBody) {
                    body.linearVelocity = Vector3f.ZERO
                    body.angularVelocity = Vector3f.ZERO
                }*/
            }
        }
    }
}

@All(Held::class, IsMob::class)
class HeldMobSystem(
    private val sokol: Sokol,
    ids: ComponentIdAccess
) : SokolSystem {
    private val mHeld = ids.mapper<Held>()

    @Subscribe
    fun on(event: MobEvent.RemoveFromWorld, entity: SokolEntity) {
        val (hold) = mHeld.get(entity)

        sokol.holding.stop(hold)
    }
}
