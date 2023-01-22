package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.with
import com.gitlab.aecsocket.alexandria.core.extension.clamp
import com.gitlab.aecsocket.alexandria.core.physics.*
import com.gitlab.aecsocket.alexandria.paper.alexandria
import com.gitlab.aecsocket.alexandria.paper.extension.*
import com.gitlab.aecsocket.craftbullet.paper.CraftBulletAPI
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.*
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.spongepowered.configurate.objectmapping.ConfigSerializable

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
            //ctx.system { HoldDetachableLocalSystem(it) }
        }
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
    ) : SimpleComponentProfile<HoldDetachable> {
        override val componentType get() = HoldDetachable::class

        val detachAxisNorm = detachAxis.normalized

        override fun createEmpty() = ComponentBlueprint { HoldDetachable(this) }
    }
}

/*@All(HoldDetachable::class)
@Before(DeltaTransformTarget::class)
class HoldDetachableLocalSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mHoldDetachable = ids.mapper<HoldDetachable>()
    private val mDeltaTransform = ids.mapper<DeltaTransform>()

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        val holdDetachable = mHoldDetachable.get(entity)
        mDeltaTransform.combine(entity, holdDetachable.localTransform)
    }
}*/ // todo wtf does this do??

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
    private val mDeltaTransform = ids.mapper<DeltaTransform>()

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
        val holdDetachable = mHoldDetachable.get(entity)
        val colliderInstance = mColliderInstance.get(entity)

        if (!holdDetachable.profile.hasCollision) {
            // only call from this entity down, so the parent's contact response isn't changed
            entity.call(ColliderInstanceSystem.ChangeContactResponse(!isHeld))
        }

        colliderInstance.parentJoint?.let { joint ->
            joint.isEnabled = !isHeld
        }
    }

    @Subscribe
    fun on(event: ColliderSystem.CreatePhysics, entity: SokolEntity) {
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
        val holdDetachable = mHoldDetachable.get(entity)
        val (hold) = mHeld.get(entity)
        val isChild = mIsChild.get(entity)
        val pPositionAccess = mPositionAccess.getOr(isChild.parent) ?: return
        val player = hold.player

        val operation = hold.operation as? DetachHoldOperation ?: return
        if (hold.frozen) return

        val pTransform = pPositionAccess.transform
        val planeOrigin = pTransform.position

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

            val relative = Transform(axis * clamp(distanceAlongAxis, 0.0, holdDetachable.profile.stopAt))
            holdDetachable.localTransform = relative
            // todo how do we apply a local transform for the next tick?

            val transform = pTransform * relative
            hold.nextTransform = transform
            player.spawnParticle(Particle.WATER_BUBBLE, transform.position.location(player.world), 0)

            if (distanceAlongAxis >= holdDetachable.profile.detachAt) {
                // todo do this stupid f'n detach
                sokol.scheduleDelayed {
                    compositeMutator.detach(entity)
                    sokol.hoster.hostMob(entity, player.world, transform, construct = false)
                    sokol.holding.start(player.alexandria, entity, MoveHoldOperation(), transform)
                    entity.call(DetachFrom)
                }
            } else {
            }
        }
    }
}
