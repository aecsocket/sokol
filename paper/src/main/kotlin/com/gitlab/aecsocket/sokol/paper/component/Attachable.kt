package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.*
import com.gitlab.aecsocket.alexandria.paper.extension.*
import com.gitlab.aecsocket.craftbullet.core.physPosition
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.extension.bullet
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import com.jme3.bullet.collision.shapes.SphereCollisionShape
import com.jme3.bullet.objects.PhysicsGhostObject
import org.spongepowered.configurate.ConfigurationNode

object Attachable : PersistentComponent {
    override val componentType get() = Attachable::class
    override val key = SokolAPI.key("attachable")
    val Type = ComponentType.singletonComponent(key, Attachable)

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()

    override fun write(node: ConfigurationNode) {}
}

@All(Attachable::class, Holdable::class, Collider::class)
@Before(OnInputSystem::class)
class AttachableSystem(mappers: ComponentIdAccess) : SokolSystem {
    companion object {
        val Attach = SokolAPI.key("attachable/attach")
    }

    private val mHoldable = mappers.componentMapper<Holdable>()
    private val mCollider = mappers.componentMapper<Collider>()
    private val mEntitySlot = mappers.componentMapper<EntitySlot>()
    private val mPosition = mappers.componentMapper<PositionRead>()
    private val mComposite = mappers.componentMapper<Composite>()
    private val mCompositePathed = mappers.componentMapper<CompositePathed>()

    @Subscribe
    fun on(event: HoldableSystem.ComputeState, entity: SokolEntity) {
        val holdable = mHoldable.get(entity)
        val collider = mCollider.get(entity)
        val holdProfile = holdable.profile
        val holdState = holdable.state ?: return
        val (_, physSpace) = collider.body ?: return
        val player = holdState.player

        val location = player.eyeLocation
        val ray = Ray(location.position(), location.direction())

        data class SlotBody(
            val entity: SokolEntity,
            val path: CompositePath,
            val transform: Transform,
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
                    val slotTransform = mPosition.getOr(entity)?.transform ?: return
                    val compositePath = mCompositePathed.getOr(entity)?.path ?: return

                    testRayShape(slotTransform.invert(ray), entitySlot.profile.shape)?.let { collision ->
                        slotBodies.add(SlotBody(entity, compositePath, slotTransform, collision.tIn))
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
        event.transform = slotResult.transform
    }

    @Subscribe
    fun on(event: OnInputSystem.Build, entity: SokolEntity) {
        val holdable = mHoldable.get(entity)

        event.addAction(Attach) { (player, _, cancel) ->
            false
        }
    }
}
