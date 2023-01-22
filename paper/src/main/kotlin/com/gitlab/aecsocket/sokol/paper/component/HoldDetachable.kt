package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.with
import com.gitlab.aecsocket.alexandria.core.extension.clamp
import com.gitlab.aecsocket.alexandria.core.physics.*
import com.gitlab.aecsocket.alexandria.paper.alexandria
import com.gitlab.aecsocket.alexandria.paper.extension.*
import com.gitlab.aecsocket.craftbullet.paper.CraftBulletAPI
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.*
import org.bukkit.entity.Player
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required

class DetachHoldOperation : HoldOperation {
    override val canRelease get() = true
}

data class HoldDetachable(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("hold_detachable")
        val Type = ComponentType.deserializing(Key, Profile::class)

        fun init(ctx: Sokol.InitContext) {
            ctx.persistentComponent(Type)
            ctx.system { HoldDetachableSystem(ctx.sokol, it).init(ctx) }
        }
    }

    override val componentType get() = HoldDetachable::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        @Required val detachAxis: Vector3,
        @Required val stopAt: Double,
        @Required val detachAt: Double,
        val disableCollision: Boolean = false
    ) : SimpleComponentProfile<HoldDetachable> {
        override val componentType get() = HoldDetachable::class

        val detachAxisNorm = detachAxis.normalized

        override fun createEmpty() = ComponentBlueprint { HoldDetachable(this) }
    }
}

@All(HoldDetachable::class, Held::class, ColliderInstance::class, IsChild::class, PositionAccess::class)
@After(ColliderInstanceTarget::class, PositionAccessTarget::class)
class HoldDetachableSystem(
    private val sokol: Sokol,
    ids: ComponentIdAccess
) : SokolSystem {
    companion object {
        val Start = HoldDetachable.Key.with("start")
    }

    private val compositeMutator = CompositeMutator(ids)
    private val mHoldDetachable = ids.mapper<HoldDetachable>()
    private val mHeld = ids.mapper<Held>()
    private val mColliderInstance = ids.mapper<ColliderInstance>()
    private val mIsChild = ids.mapper<IsChild>()
    private val mPositionAccess = ids.mapper<PositionAccess>()
    private val mPositionFromParent = ids.mapper<PositionFromParent>()
    private val mComposite = ids.mapper<Composite>()

    object DetachFrom : SokolEvent

    internal fun init(ctx: Sokol.InitContext): HoldDetachableSystem {
        ctx.components.entityCallbacks.apply {
            callback(Start, ::start)
        }
        return this
    }

    private fun start(entity: SokolEntity, player: Player): Boolean {
        if (!mHoldDetachable.has(entity)) return false
        val positionAccess = mPositionAccess.getOr(entity) ?: return false

        if (mHeld.has(entity)) return false // this entity is already held
        if (!mIsChild.has(entity)) return false
        sokol.holding.start(player.alexandria, entity, DetachHoldOperation(), positionAccess.transform)
        return true
    }

    private fun updateBody(entity: SokolEntity, isHeld: Boolean) {
        val holdDetachable = mHoldDetachable.get(entity).profile
        val colliderInstance = mColliderInstance.get(entity)

        if (holdDetachable.disableCollision) {
            // only call from this entity down, so the parent's contact response isn't changed
            mComposite.forwardAll(entity, ColliderInstanceSystem.ChangeContactResponse(!isHeld))
        }

        mPositionFromParent.getOr(entity)?.let { positionFromParent ->
            positionFromParent.enabled = !isHeld
        }

        colliderInstance.parentJoint?.let { joint ->
            joint.isEnabled = !isHeld
        }
    }

    @Subscribe
    fun on(event: ColliderSystem.CreatePhysics, entity: SokolEntity) {
        val (hold) = mHeld.get(entity)
        if (hold.operation !is DetachHoldOperation) return

        updateBody(entity, true)
    }

    @Subscribe
    fun on(event: EntityHolding.ChangeHoldState, entity: SokolEntity) {
        val (hold) = mHeld.get(entity)
        if (hold.operation !is DetachHoldOperation) return

        CraftBulletAPI.executePhysics {
            updateBody(entity, event.held)
        }
    }

    @Subscribe
    fun on(event: ColliderSystem.PrePhysicsStep, entity: SokolEntity) {
        val holdDetachable = mHoldDetachable.get(entity).profile
        val (hold) = mHeld.get(entity)
        val isChild = mIsChild.get(entity)
        if (hold.operation !is DetachHoldOperation) return
        if (hold.frozen) return
        val pPositionAccess = mPositionAccess.getOr(isChild.parent) ?: return
        val player = hold.player

        val eye = player.eyeLocation
        val from = eye.position()
        val dir = eye.direction()
        val transform = pPositionAccess.transform
        val planeOrigin = transform.position

        val ray = Ray(from - planeOrigin, dir)
        val plane = PlaneShape((planeOrigin - from).normalized)
        val (tIn) = plane.testRay(ray) ?: return
        val intersect = from + dir * tIn
        val detachAxis = holdDetachable.detachAxisNorm
        val distanceAlongAxis = transform.invert(intersect).dot(detachAxis)

        val relative = Transform(detachAxis * clamp(distanceAlongAxis, 0.0, holdDetachable.stopAt))
        val nextTransform = transform * relative
        hold.nextTransform = nextTransform

        if (distanceAlongAxis >= holdDetachable.detachAt) {
            sokol.scheduleDelayed {
                compositeMutator.detach(entity)
                sokol.hoster.hostMob(entity, player.world, nextTransform, construct = false)
                sokol.holding.start(player.alexandria, entity, MoveHoldOperation(), nextTransform)
                entity.call(DetachFrom)
            }
        }
    }
}
