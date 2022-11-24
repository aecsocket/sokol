package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.Euler3
import com.gitlab.aecsocket.alexandria.core.extension.EulerOrder
import com.gitlab.aecsocket.alexandria.core.extension.quaternion
import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.core.physics.Vector3
import com.gitlab.aecsocket.alexandria.core.physics.quaternionFromTo
import com.gitlab.aecsocket.alexandria.core.physics.quaternionOfAxes
import com.gitlab.aecsocket.alexandria.paper.extension.*
import com.gitlab.aecsocket.craftbullet.core.TrackedPhysicsObject
import com.gitlab.aecsocket.craftbullet.core.hitNormal
import com.gitlab.aecsocket.craftbullet.core.radians
import com.gitlab.aecsocket.craftbullet.core.transform
import com.gitlab.aecsocket.craftbullet.paper.rayTestFrom
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.extension.alexandria
import com.gitlab.aecsocket.sokol.core.extension.bullet
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import kotlin.math.PI
import kotlin.math.abs

data class HeldSnap(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("held_snap")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = HeldSnap::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        val snapTransform: Transform = Transform.Identity,
        val snapDistance: Double = 0.0,
        val allowFreePlace: Boolean = true
    ) : SimpleComponentProfile {
        override val componentType get() = HeldSnap::class

        override fun createEmpty() = ComponentBlueprint { HeldSnap(this) }
    }
}

@All(HeldSnap::class, Held::class, ColliderInstance::class)
@After(ColliderInstanceTarget::class, HoldMovableColliderSystem::class)
class HeldSnapSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mHeldSnap = ids.mapper<HeldSnap>()
    private val mHeld = ids.mapper<Held>()
    private val mColliderInstance = ids.mapper<ColliderInstance>()

    @Subscribe
    fun on(event: ColliderSystem.PostPhysicsStep, entity: SokolEntity) {
        val heldSnap = mHeldSnap.get(entity).profile
        val (hold) = mHeld.get(entity)
        val (physObj) = mColliderInstance.get(entity)
        val body = physObj.body
        val player = hold.player

        val operation = hold.operation
        if (operation !is MoveHoldOperation) return

        val from = player.eyeLocation
        val direction = from.direction.alexandria()

        val rayTest = player.rayTestFrom(heldSnap.snapDistance.toFloat())
            .firstOrNull {
                val obj = it.collisionObject
                obj !is TrackedPhysicsObject || obj.id != obj.id
            }

        if (rayTest == null) {
            // suspended in mid-air
            operation.canRelease = heldSnap.allowFreePlace
        } else {
            operation.canRelease = true

            // the hit normal is facing from the surface, to the player
            // but when holding (non-snap) it's the opposite direction
            // so we invert the normal here to face it in the right direction
            val hitPos = from.position() + direction * (heldSnap.snapDistance * rayTest.hitFraction)
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

            operation.nextTransform = Transform(hitPos, rotation) + heldSnap.snapTransform
        }
    }
}
