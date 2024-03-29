package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.with
import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.alexandria
import com.gitlab.aecsocket.alexandria.paper.extension.*
import com.gitlab.aecsocket.craftbullet.core.*
import com.gitlab.aecsocket.craftbullet.paper.CraftBulletAPI
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.*
import org.bukkit.entity.Player
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required

class MoveHoldOperation : HoldOperation {
    override var canRelease = false
}

data class HoldMovable(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("hold_movable")
        val Type = ComponentType.deserializing(Key, Profile::class)

        fun init(ctx: Sokol.InitContext) {
            ctx.persistentComponent(Type)
            ctx.system { HoldMovableCallbackSystem(ctx.sokol, it).init(ctx) }
            ctx.system { HoldMovableColliderSystem(it) }
        }
    }

    override val componentType get() = HoldMovable::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        @Required val holdDistance: Double,
        val holdTransform: Transform = Transform.Identity,
        val disableCollision: Boolean = false
    ) : SimpleComponentProfile<HoldMovable> {
        override val componentType get() = HoldMovable::class

        override fun createEmpty() = ComponentBlueprint { HoldMovable(this) }
    }
}

class HoldMovableCallbackSystem(
    private val sokol: Sokol,
    ids: ComponentIdAccess
) : SokolSystem {
    companion object {
        val Start = HoldMovable.Key.with("start")
    }

    private val mHoldMovable = ids.mapper<HoldMovable>()
    private val mHeld = ids.mapper<Held>()
    private val mPositionAccess = ids.mapper<PositionAccess>()

    internal fun init(ctx: Sokol.InitContext): HoldMovableCallbackSystem {
        ctx.components.callbacks.apply {
            callback(Start, ::start)
        }
        return this
    }

    private fun start(entity: SokolEntity, player: Player): Boolean {
        if (!mHoldMovable.has(entity)) return false
        val positionAccess = mPositionAccess.getOr(entity) ?: return false

        if (mHeld.has(entity)) return false // this entity is already held
        sokol.holding.start(player.alexandria, entity, MoveHoldOperation(), positionAccess.transform)
        return true
    }
}

@All(HoldMovable::class, Held::class)
@Before(HeldColliderSystem::class)
@After(ColliderInstanceTarget::class)
class HoldMovableColliderSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mHoldMovable = ids.mapper<HoldMovable>()
    private val mHeld = ids.mapper<Held>()
    private val mIsChild = ids.mapper<IsChild>()
    private val mComposite = ids.mapper<Composite>()

    private fun updateBody(entity: SokolEntity, isHeld: Boolean) {
        val holdMovable = mHoldMovable.get(entity)

        if (holdMovable.profile.disableCollision) {
            // call from the root down, so all entities in this tree have their collision response updated
            mComposite.forwardAll(mIsChild.root(entity), ColliderInstanceSystem.ChangeContactResponse(!isHeld))
        }
    }

    @Subscribe
    fun on(event: ColliderSystem.CreatePhysics, entity: SokolEntity) {
        val (hold) = mHeld.get(entity)
        if (hold.operation !is MoveHoldOperation) return

        updateBody(entity, true)
    }

    @Subscribe
    fun on(event: EntityHolding.ChangeHoldState, entity: SokolEntity) {
        val (hold) = mHeld.get(entity)
        if (hold.operation !is MoveHoldOperation) return

        CraftBulletAPI.executePhysics {
            updateBody(entity, event.held)
        }
    }

    @Subscribe
    fun on(event: ColliderSystem.PrePhysicsStep, entity: SokolEntity) {
        val holdMovable = mHoldMovable.get(entity)
        val (hold) = mHeld.get(entity)
        if (hold.operation !is MoveHoldOperation) return
        if (hold.frozen) return
        val player = hold.player

        val from = player.eyeLocation
        val direction = from.direction.alexandria()

        hold.nextTransform = Transform(
            (from + direction * holdMovable.profile.holdDistance).position(),
            from.rotation()
        ) * holdMovable.profile.holdTransform
    }
}
