package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.with
import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.alexandria
import com.gitlab.aecsocket.alexandria.paper.extension.*
import com.gitlab.aecsocket.craftbullet.core.*
import com.gitlab.aecsocket.craftbullet.paper.CraftBulletAPI
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.*
import com.jme3.bullet.objects.PhysicsRigidBody
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required

data class HoldMovable(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("hold_movable")
        val Type = ComponentType.deserializing(Key, Profile::class)
    }

    override val componentType get() = HoldMovable::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        @Required val holdDistance: Double,
        val holdTransform: Transform = Transform.Identity,
        val hasCollision: Boolean = true
    ) : SimpleComponentProfile<HoldMovable> {
        override val componentType get() = HoldMovable::class

        override fun createEmpty() = ComponentBlueprint { HoldMovable(this) }
    }
}

class MoveHoldOperation : HoldOperation {
    override var canRelease = false
}

@All(HoldMovable::class, InputCallbacks::class, PositionAccess::class)
@Before(InputCallbacksSystem::class)
@After(PositionAccessTarget::class)
class HoldMovableCallbackSystem(
    private val sokol: Sokol,
    ids: ComponentIdAccess
) : SokolSystem {
    companion object {
        val Start = HoldMovable.Key.with("start")
    }

    private val mInputCallbacks = ids.mapper<InputCallbacks>()
    private val mHeld = ids.mapper<Held>()
    private val mPositionAccess = ids.mapper<PositionAccess>()

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        val inputCallbacks = mInputCallbacks.get(entity)
        val positionAccess = mPositionAccess.get(entity)

        inputCallbacks.callback(Start) { player ->
            if (mHeld.has(entity)) // this entity is already held
                return@callback false

            sokol.holding.start(player.alexandria, entity, MoveHoldOperation(), positionAccess.transform)
            true
        }
    }
}

@All(HoldMovable::class, Held::class)
@After(ColliderInstanceTarget::class)
class HoldMovableColliderSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mHoldMovable = ids.mapper<HoldMovable>()
    private val mHeld = ids.mapper<Held>()
    private val mIsChild = ids.mapper<IsChild>()

    private fun updateBody(entity: SokolEntity, isHeld: Boolean) {
        val holdMovable = mHoldMovable.get(entity)

        if (!holdMovable.profile.hasCollision) {
            // call from the root down, so all entities in this tree have their collision response updated
            mIsChild.root(entity)
                .call(ColliderInstanceSystem.ChangeContactResponse(!isHeld))
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
        val holdMovable = mHoldMovable.get(entity)
        val (hold) = mHeld.get(entity)
        val player = hold.player

        val operation = hold.operation as? MoveHoldOperation ?: return
        if (hold.frozen) return

        val from = player.eyeLocation
        val direction = from.direction.alexandria()

        hold.nextTransform = Transform(
            (from + direction * holdMovable.profile.holdDistance).position(),
            from.rotation()
        ) * holdMovable.profile.holdTransform
    }
}
