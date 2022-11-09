package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.*
import com.gitlab.aecsocket.alexandria.paper.AlexandriaAPI
import com.gitlab.aecsocket.alexandria.paper.SoundEngineEffect
import com.gitlab.aecsocket.alexandria.paper.extension.*
import com.gitlab.aecsocket.craftbullet.core.physPosition
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.extension.bullet
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import com.jme3.bullet.collision.shapes.SphereCollisionShape
import com.jme3.bullet.objects.PhysicsGhostObject
import org.spongepowered.configurate.objectmapping.ConfigSerializable

data class Attachable(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("attachable")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = Attachable::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        val soundAttach: SoundEngineEffect = SoundEngineEffect.Empty,
    ) : SimpleComponentProfile {
        override fun readEmpty() = Attachable(this)
    }
}

@All(Attachable::class, Holdable::class, Collider::class, Removable::class, PositionRead::class)
@Before(OnInputSystem::class)
@After(HoldableMovementSystem::class)
class AttachableSystem(mappers: ComponentIdAccess) : SokolSystem {
    companion object {
        val Attach = SokolAPI.key("attachable/attach")
    }

    private val mAttachable = mappers.componentMapper<Attachable>()
    private val mHoldable = mappers.componentMapper<Holdable>()
    private val mCollider = mappers.componentMapper<Collider>()
    private val mAsChildTransform = mappers.componentMapper<AsChildTransform>()
    private val mRemovable = mappers.componentMapper<Removable>()
    private val mPositionRead = mappers.componentMapper<PositionRead>()
    private val mEntitySlot = mappers.componentMapper<EntitySlot>()
    private val mComposite = mappers.componentMapper<Composite>()
    private val mCompositeChild = mappers.componentMapper<CompositeChild>()
    private val mSupplierEntityAccess = mappers.componentMapper<SupplierEntityAccess>()

    @Subscribe
    fun on(event: HoldableMovementSystem.UpdatePosition, entity: SokolEntity) {
        val holdable = mHoldable.get(entity)
        val collider = mCollider.get(entity)
        val asChildTransform = mAsChildTransform.getOr(entity)?.profile?.transform ?: Transform.Identity
        val holdProfile = holdable.profile
        val holdState = holdable.state ?: return
        val holdOp = holdState.operation as? MovingHoldOperation ?: return
        val (_, physSpace) = collider.body ?: return
        val player = holdState.player

        val location = player.eyeLocation
        val ray = Ray(location.position(), location.direction())

        data class SlotBody(
            val entity: SokolEntity,
            val path: CompositePath,
            val transform: Transform,
            val accepts: Boolean,
            val tIn: Double,
        )

        val ghost = PhysicsGhostObject(SphereCollisionShape(holdProfile.fSnapDistance)).also {
            it.physPosition = location.position().bullet()
            physSpace.addCollisionObject(it)
        }
        val slotBodies = ArrayList<SlotBody>()

        ghost.overlappingObjects.forEach { obj ->
            if (obj is SokolPhysicsObject) {
                val testEntity = obj.entity
                if (entity === testEntity) return@forEach

                fun act(entity: SokolEntity) {
                    val entitySlot = mEntitySlot.getOr(entity) ?: return
                    val slotTransform = mPositionRead.getOr(entity)?.transform ?: return
                    val composite = mComposite.getOr(entity) ?: return
                    val compositePath = mCompositeChild.getOr(entity)?.path ?: return
                    if (composite.children().isNotEmpty()) return

                    testRayShape(slotTransform.invert(ray), entitySlot.profile.shape)?.let { collision ->
                        slotBodies.add(SlotBody(testEntity, compositePath, slotTransform, entitySlot.profile.accepts, collision.tIn))
                    }
                }

                fun walk(entity: SokolEntity) {
                    act(entity)
                    mComposite.forEachChild(entity) { (_, child) ->
                        walk(child)
                    }
                }

                walk(testEntity)
            }
        }
        physSpace.removeCollisionObject(ghost)

        val slotResult = slotBodies.minByOrNull { it.tIn } ?: return
        if (slotResult.accepts) {
            holdOp.placing = MovingPlaceState.ALLOW_ATTACH
            holdOp.transform = slotResult.transform + asChildTransform
            holdOp.attachTo = HoldAttach(slotResult.entity, slotResult.path)
        } else {
            holdOp.placing = MovingPlaceState.DISALLOW_ATTACH
        }
    }

    @Subscribe
    fun on(event: OnInputSystem.Build, entity: SokolEntity) {
        val attachable = mAttachable.get(entity).profile
        val holdable = mHoldable.get(entity)
        val removable = mRemovable.get(entity)
        val position = mPositionRead.get(entity)
        val holdOp = holdable.state?.operation as? MovingHoldOperation ?: return

        event.addAction(Attach) { (_, _, cancel) ->
            holdOp.attachTo?.let { attachTo ->
                cancel()
                mSupplierEntityAccess.getOr(attachTo.entity)?.useEntity { rootEntity ->
                    val parentEntity = mComposite.child(rootEntity, attachTo.path) ?: return@useEntity
                    val parentComposite = mComposite.getOr(parentEntity) ?: return@useEntity
                    val entitySlot = mEntitySlot.getOr(parentEntity) ?: return@useEntity
                    if (!entitySlot.profile.accepts) return@useEntity

                    AlexandriaAPI.soundEngine.play(position.location(), attachable.soundAttach)
                    entity.call(SokolEvent.Reset)
                    removable.remove()
                    parentComposite.attach(ENTITY_SLOT_CHILD_KEY, entity)
                    entity.call(CompositeSystem.AttachTo(ENTITY_SLOT_CHILD_KEY, parentEntity, rootEntity, true))
                    rootEntity.call(CompositeSystem.TreeMutate)
                }
                true
            } ?: false
        }
    }
}
