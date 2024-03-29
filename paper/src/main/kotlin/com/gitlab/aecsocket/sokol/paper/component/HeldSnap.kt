package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.Euler3
import com.gitlab.aecsocket.alexandria.core.extension.EulerOrder
import com.gitlab.aecsocket.alexandria.core.extension.quaternion
import com.gitlab.aecsocket.alexandria.core.physics.*
import com.gitlab.aecsocket.alexandria.paper.extension.*
import com.gitlab.aecsocket.craftbullet.core.TrackedPhysicsObject
import com.gitlab.aecsocket.craftbullet.core.hitNormal
import com.gitlab.aecsocket.craftbullet.core.radians
import com.gitlab.aecsocket.craftbullet.paper.rayTestFrom
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.extension.alexandria
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import com.gitlab.aecsocket.sokol.paper.persistentComponent
import com.simsilica.mathd.Vec3d
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required
import kotlin.math.PI
import kotlin.math.abs

data class HeldSnap(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("held_snap")
        val Type = ComponentType.deserializing(Key, Profile::class)

        fun init(ctx: Sokol.InitContext) {
            ctx.persistentComponent(Type)
            ctx.system { HeldSnapSystem(it) }
        }
    }

    data class SurfaceData(
        val normal: Vec3d,
        val rotation: Quaternion,
    )

    override val componentType get() = HeldSnap::class
    override val key get() = Key

    var isSnapping = false
    var lastSurface: SurfaceData? = null

    @ConfigSerializable
    data class Profile(
        @Required val snapDistance: Double,
        val snapTransform: Transform = Transform.Identity,
        val allowFreePlace: Boolean = true
    ) : SimpleComponentProfile<HeldSnap> {
        override val componentType get() = HeldSnap::class

        override fun createEmpty() = ComponentBlueprint { HeldSnap(this) }
    }
}

@All(HeldSnap::class, Held::class, ColliderInstance::class)
@Before(HeldColliderSystem::class)
@After(ColliderInstanceTarget::class, HoldMovableColliderSystem::class)
class HeldSnapSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mHeldSnap = ids.mapper<HeldSnap>()
    private val mHeld = ids.mapper<Held>()
    private val mColliderInstance = ids.mapper<ColliderInstance>()
    private val mIsChild = ids.mapper<IsChild>()

    data class ChangeSnapping(
        val isSnapping: Boolean
    ) : SokolEvent

    @Subscribe
    fun on(event: ColliderSystem.PrePhysicsStep, entity: SokolEntity) {
        val heldSnap = mHeldSnap.get(entity)
        val (hold) = mHeld.get(entity)
        val (physObj) = mColliderInstance.get(entity)
        val player = hold.player

        val operation = hold.operation as? MoveHoldOperation ?: return
        if (hold.frozen) return

        val from = player.eyeLocation
        val direction = from.direction.alexandria()

        val root = mIsChild.root(entity)
        val rayTest = player.rayTestFrom(heldSnap.profile.snapDistance)
            .firstOrNull {
                val obj = it.collisionObject
                // don't snap to bodies on our own tree
                obj !is SokolPhysicsObject || (mIsChild.root(obj.entity) !== root)
            }

        val isSnapping = if (rayTest == null) {
            operation.canRelease = heldSnap.profile.allowFreePlace
            false
        } else {
            operation.canRelease = true

            val hitPos = from.position() + direction * (heldSnap.profile.snapDistance * rayTest.hitFraction)
            val hitNormal = rayTest.hitNormal

            // the hit normal is facing from the surface, to the player
            // but when holding (non-snap) it's the opposite direction
            // so we invert the normal here to face it in the right direction
            val dir = -rayTest.hitNormal.alexandria()
            val rotation = if (abs(dir.dot(Vector3.Up)) > 0.99) {
                // `dir` and `up` are (close to) collinear
                val yaw = radians(player.location.yaw).toDouble()
                // `rotation` will be facing "away" from the player
                quaternionFromTo(
                    Vector3.Forward, dir
                ) * Euler3(z = (if (dir.y > 0.0) -yaw else yaw) + 2*PI).quaternion(EulerOrder.XYZ)
            } else {
                heldSnap.lastSurface?.let { lastSurface ->
                    if (lastSurface.normal.dot(hitNormal) > 0.99) {
                        // we're still on the same surface
                        // keep the same rotation
                        return@let lastSurface.rotation
                    }
                    null
                } ?: run {
                    val v1 = Vector3.Up.cross(dir).normalized
                    val v2 = dir.cross(v1).normalized
                    quaternionOfAxes(v1, v2, dir)
                }
            }

            heldSnap.lastSurface = HeldSnap.SurfaceData(hitNormal, rotation)
            hold.nextTransform = Transform(hitPos, rotation) * heldSnap.profile.snapTransform
            true
        }

        if (isSnapping != heldSnap.isSnapping) {
            heldSnap.isSnapping = isSnapping
            entity.call(ChangeSnapping(isSnapping))
        }
    }
}
