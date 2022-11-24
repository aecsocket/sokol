package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.with
import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.alexandria
import com.gitlab.aecsocket.alexandria.paper.extension.*
import com.gitlab.aecsocket.craftbullet.core.angularVelocity
import com.gitlab.aecsocket.craftbullet.core.linearVelocity
import com.gitlab.aecsocket.craftbullet.core.transform
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.extension.bullet
import com.gitlab.aecsocket.sokol.paper.*
import com.jme3.bullet.objects.PhysicsRigidBody
import com.jme3.math.Vector3f
import org.spongepowered.configurate.objectmapping.ConfigSerializable

data class HoldMovable(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("hold_movable")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = HoldMovable::class
    override val key get() = Key

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

        inputCallbacks.callback(StartMove) { input ->
            if (mHeld.has(entity)) // this entity is already held
                return@callback false

            sokol.holding.start(input.player.alexandria, entity, MoveHoldOperation(positionRead.transform))
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
    fun on(event: ColliderSystem.PostPhysicsStep, entity: SokolEntity) {
        val holdMovable = mHoldMovable.get(entity).profile
        val (hold) = mHeld.get(entity)
        val (physObj) = mColliderInstance.get(entity)
        val body = physObj.body
        val player = hold.player

        val operation = hold.operation as? MoveHoldOperation ?: return
        if (hold.frozen) return

        body.transform = operation.nextTransform.bullet()
        if (body is PhysicsRigidBody) {
            body.linearVelocity = Vector3f.ZERO
            body.angularVelocity = Vector3f.ZERO
        }

        val from = player.eyeLocation
        val direction = from.direction.alexandria()

        operation.nextTransform = Transform(
            (from + direction * holdMovable.holdDistance).position(),
            from.rotation()
        ) + holdMovable.holdTransform
    }
}
