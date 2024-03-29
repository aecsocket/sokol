package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.with
import com.gitlab.aecsocket.alexandria.core.physics.Ray
import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.core.physics.invert
import com.gitlab.aecsocket.alexandria.paper.extension.direction
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.alexandria.paper.extension.position
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import com.gitlab.aecsocket.sokol.paper.persistentComponent
import com.jme3.bullet.objects.PhysicsRigidBody
import org.bukkit.entity.Player
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required

data class HeldAttachable(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("held_attachable")
        val Type = ComponentType.deserializing(Key, Profile::class)

        fun init(ctx: Sokol.InitContext) {
            ctx.persistentComponent(Type)
            ctx.system { HeldAttachableSystem(ctx.sokol, it).init(ctx) }
        }
    }

    data class AttachTo(
        val target: SokolEntity,
        val allows: Boolean,
        val slot: EntitySlot
    )

    override val componentType get() = HeldAttachable::class
    override val key get() = Key

    var attachTo: AttachTo? = null

    @ConfigSerializable
    data class Profile(
        @Required val attachDistance: Double
    ) : SimpleComponentProfile<HeldAttachable> {
        override val componentType get() = HeldAttachable::class

        override fun createEmpty() = ComponentBlueprint { HeldAttachable(this) }
    }
}

@All(HeldAttachable::class, Held::class, ColliderInstance::class)
@Before(HeldColliderSystem::class)
@After(ColliderInstanceTarget::class, HoldMovableColliderSystem::class, HeldSnapSystem::class, EntitySlotTarget::class)
class HeldAttachableSystem(
    private val sokol: Sokol,
    ids: ComponentIdAccess
) : SokolSystem {
    companion object {
        val Attach = HeldAttachable.Key.with("attach")
    }

    private val mHeldAttachable = ids.mapper<HeldAttachable>()
    private val mRemovable = ids.mapper<Removable>()
    private val mHeld = ids.mapper<Held>()
    private val mColliderInstance = ids.mapper<ColliderInstance>()
    private val mIsChild = ids.mapper<IsChild>()
    private val mDeltaTransform = ids.mapper<DeltaTransform>()
    private val mEntitySlot = ids.mapper<EntitySlot>()
    private val mPositionAccess = ids.mapper<PositionAccess>()

    object AttachTo : SokolEvent

    object ChangeAttachTo : SokolEvent

    internal fun init(ctx: Sokol.InitContext): HeldAttachableSystem {
        ctx.components.callbacks.apply {
            callback(Attach, ::attach)
        }
        return this
    }

    private fun attach(entity: SokolEntity, player: Player): Boolean {
        val heldAttachable = mHeldAttachable.getOr(entity) ?: return false
        val removable = mRemovable.getOr(entity) ?: return false
        val (hold) = mHeld.getOr(entity) ?: return false

        if (player !== hold.player) return false
        val attachTo = heldAttachable.attachTo ?: return false
        if (!attachTo.allows) return true

        removable.remove(true)
        attachTo.slot.attach(entity)

        heldAttachable.attachTo = null
        sokol.holding.stop(hold)
        entity.call(AttachTo)
        return true
    }

    @Subscribe
    fun on(event: ColliderSystem.PrePhysicsStep, entity: SokolEntity) {
        val heldAttachable = mHeldAttachable.get(entity)
        val (hold) = mHeld.get(entity)
        val (physObj, physSpace) = mColliderInstance.get(entity)
        val body = physObj.body as? PhysicsRigidBody ?: return
        val localTransform = mDeltaTransform.getOr(entity)?.transform ?: Transform.Identity
        val player = hold.player

        if (hold.operation !is MoveHoldOperation) return
        if (hold.frozen) return

        val from = player.eyeLocation
        val ray = Ray(from.position(), from.direction())

        data class SlotBody(
            val entity: SokolEntity,
            val tIn: Double,
            val slot: EntitySlot,
            val transform: Transform
        )

        val slotBodies = ArrayList<SlotBody>()
        val nearby = sokol.resolver.entitiesNear(physSpace, from.position(), heldAttachable.profile.attachDistance)
        val root = mIsChild.root(entity)
        nearby.forEach { testEntity ->
            if (mIsChild.root(testEntity) === root) return@forEach
            val testSlot = mEntitySlot.getOr(testEntity) ?: return@forEach
            if (testSlot.full()) return@forEach
            val testTransform = mPositionAccess.getOr(testEntity)?.transform ?: return@forEach

            val collision = testSlot.shape.testRay(testTransform.invert(ray)) ?: return@forEach
            if (collision.tIn > heldAttachable.profile.attachDistance) return@forEach
            slotBodies.add(SlotBody(testEntity, collision.tIn, testSlot, testTransform))
        }

        val attachTo = heldAttachable.attachTo
        val newAttachTo = slotBodies.minByOrNull { it.tIn }?.let { slot ->
            val allows = slot.slot.allows(entity)
            if (allows)
                hold.nextTransform = slot.transform * localTransform
            HeldAttachable.AttachTo(slot.entity, allows, slot.slot)
        }

        if (newAttachTo != attachTo) {
            heldAttachable.attachTo = newAttachTo
            body.isKinematic = newAttachTo != null
            entity.call(ChangeAttachTo)
        }
    }
}
