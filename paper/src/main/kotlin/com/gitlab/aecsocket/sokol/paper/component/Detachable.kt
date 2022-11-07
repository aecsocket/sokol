package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.clamp
import com.gitlab.aecsocket.alexandria.core.physics.*
import com.gitlab.aecsocket.alexandria.paper.AlexandriaAPI
import com.gitlab.aecsocket.alexandria.paper.SoundEngineEffect
import com.gitlab.aecsocket.alexandria.paper.alexandria
import com.gitlab.aecsocket.alexandria.paper.extension.*
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.EntityHolding
import com.gitlab.aecsocket.sokol.paper.HoldOperation
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import com.gitlab.aecsocket.sokol.paper.util.colliderHitPath
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required

data class Detachable(val profile: Profile) : MarkerPersistentComponent {
    companion object {
        val Key = SokolAPI.key("detachable")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = Detachable::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        @Required val axis: Vector3,
        @Required val detachStop: Double,
        @Required val detachAt: Double,
        val soundDetach: SoundEngineEffect = SoundEngineEffect.Empty,
    ) : NonReadingComponentProfile {
        val normAxis = axis.normalized

        override fun readEmpty() = Detachable(this)
    }
}

@All(Holdable::class)
class DetachableForwardSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mComposite = mappers.componentMapper<Composite>()

    @Subscribe
    fun on(event: OnInputSystem.Build, entity: SokolEntity) {
        mComposite.forwardAll(entity, BuildInputs { key, action -> event.addAction(key, action) })
    }

    data class BuildInputs(
        val addAction: (key: Key, action: InputAction) -> Unit
    ) : SokolEvent
}

class DetachHoldOperation(
    val path: CompositePath,
    val originalRelative: Transform,
) : HoldOperation {
    override val canRelease get() = true
}

@All(Detachable::class, CompositeChild::class, CompositeTransform::class, PositionRead::class)
@Before(OnInputSystem::class)
class DetachableSystem(
    private val sokol: Sokol,
    mappers: ComponentIdAccess
) : SokolSystem {
    companion object {
        val Detach = SokolAPI.key("detachable/detach")
    }

    private val mDetachable = mappers.componentMapper<Detachable>()
    private val mCompositeChild = mappers.componentMapper<CompositeChild>()
    private val mCompositeTransform = mappers.componentMapper<CompositeTransform>()
    private val mHoldable = mappers.componentMapper<Holdable>()
    private val mHovered = mappers.componentMapper<Hovered>()
    private val mCollider = mappers.componentMapper<Collider>()
    private val mMob = mappers.componentMapper<HostedByMob>()

    @Subscribe
    fun on(event: DetachableForwardSystem.BuildInputs, entity: SokolEntity) {
        val compositeChild = mCompositeChild.get(entity)
        val compositeTransform = mCompositeTransform.get(entity).transform
        val compositePath = compositeChild.path

        val root = compositeChild.root
        val rootHoldable = mHoldable.getOr(root) ?: return
        val rootHovered = mHovered.getOr(root) ?: return
        val rootCollider = mCollider.getOr(root) ?: return
        val rootMob = mMob.getOr(root)?.mob
        if (colliderHitPath(rootCollider, rootHovered.rayTestResult) != compositePath) return
        val holdState = rootHoldable.state

        event.addAction(Detach) { (player, _, cancel) ->
            cancel()
            if (holdState != null) return@addAction false
            sokol.entityHolding.start(player.alexandria, entity, DetachHoldOperation(
                compositePath, compositeTransform
            ), rootMob)
            true
        }
    }
}

@All(Holdable::class, Composite::class, PositionRead::class)
class DetachableHoldingSystem(
    private val sokol: Sokol,
    mappers: ComponentIdAccess
) : SokolSystem {
    private val mHoldable = mappers.componentMapper<Holdable>()
    private val mComposite = mappers.componentMapper<Composite>()
    private val mDetachable = mappers.componentMapper<Detachable>()
    private val mPositionRead = mappers.componentMapper<PositionRead>()
    private val mAsChildTransform = mappers.componentMapper<AsChildTransform>()
    private val mCompositeChild = mappers.componentMapper<CompositeChild>()

    @Subscribe
    fun on(event: HoldableSystem.Update, entity: SokolEntity) {
        val holdable = mHoldable.get(entity)
        val positionRead = mPositionRead.get(entity)
        val holdState = holdable.state ?: return
        val holdOp = holdState.operation as? DetachHoldOperation ?: return
        val child = mComposite.child(entity, holdOp.path) ?: return
        val player = holdState.player

        val detachable = mDetachable.getOr(child)?.profile ?: return
        val asChildTransform = mAsChildTransform.getOr(child) ?: return

        // to determine how far along the detach axis we've moved
        // find the intersection of:
        //  · the player's look ray
        //  · the plane with center at part, and normal facing the player

        val transform = positionRead.transform + holdOp.originalRelative
        val planeOrigin = transform.translation

        val location = player.eyeLocation
        val rayOrigin = location.position()
        val rayDirection = location.direction()
        val ray = Ray(rayOrigin - planeOrigin, rayDirection)

        val planeNormal = (rayOrigin - planeOrigin).normalized
        val plane = PlaneShape(planeNormal)

        testRayPlane(ray, plane)?.let { (tIn) ->
            val axis = detachable.normAxis
            val isect = rayOrigin + rayDirection * tIn
            val distanceAlongAxis = transform.invert(isect).dot(axis)

            val relative = Transform(axis * clamp(distanceAlongAxis, 0.0, detachable.detachStop))

            if (distanceAlongAxis >= detachable.detachAt) {
                val compositeChild = mCompositeChild.getOr(child) ?: return
                val parentComposite = mComposite.getOr(compositeChild.parent) ?: return

                parentComposite.detach(compositeChild.key)?.also {
                    entity.call(CompositeSystem.TreeMutate)
                }

                AlexandriaAPI.soundEngine.play(positionRead.location(), detachable.soundDetach)
                val blueprint = child.toBlueprint()
                mCompositeChild.remove(blueprint)

                val spawnTransform = transform + relative
                val mob = sokol.entityHoster.hostMob(blueprint, location.world, spawnTransform)
                sokol.useMob(mob) { mobEntity ->
                    sokol.entityHolding.start(player.alexandria, mobEntity, MovingHoldOperation(spawnTransform), mob)
                }
            } else {
                asChildTransform.relative = relative
            }
        }
    }

    @Subscribe
    fun on(event: EntityHolding.ChangeHoldState, entity: SokolEntity) {
        val holdOp = event.state.operation as? DetachHoldOperation ?: return
        val child = mComposite.child(entity, holdOp.path) ?: return

        val asChildTransform = mAsChildTransform.getOr(child) ?: return
        asChildTransform.relative = Transform.Identity
    }
}
