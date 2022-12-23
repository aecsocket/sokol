package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.with
import com.gitlab.aecsocket.alexandria.core.physics.Shape
import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.core.physics.transformDelta
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.craftbullet.core.*
import com.gitlab.aecsocket.craftbullet.paper.CraftBulletAPI
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.extension.*
import com.gitlab.aecsocket.sokol.paper.*
import com.jme3.bullet.collision.PhysicsCollisionObject
import com.jme3.bullet.joints.New6Dof
import com.jme3.bullet.objects.PhysicsRigidBody
import com.jme3.bullet.objects.PhysicsVehicle
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required
import java.util.UUID

object Collider : SimplePersistentComponent {
    override val componentType get() = Collider::class
    override val key = SokolAPI.key("collider")
    val Type = ComponentType.singletonComponent(key, this)
}

data class ColliderInstance(
    val physObj: SokolPhysicsObject,
    val physSpace: ServerPhysicsSpace,
    var parentJoint: New6Dof?
) : SokolComponent {
    override val componentType get() = ColliderInstance::class

    var lastTransform: Transform? = null
}

object ColliderInstanceTarget : SokolSystem

// TODO mass could probably be moved to the stats system once that's done
data class ColliderRigidBody(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("collider_rigid_body")
        val Type = ComponentType.deserializing<Profile>(Key)

        val StatMass = statKeyOf<Float>(Key.with("stat_mass"))
    }

    override val componentType get() = ColliderRigidBody::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        @Required val shape: Shape,
        val mass: Float = 1f
    ) : SimpleComponentProfile {
        override val componentType get() = ColliderRigidBody::class

        override fun createEmpty() = ComponentBlueprint { ColliderRigidBody(this) }
    }
}

object ColliderVehicleBody : SimplePersistentComponent {
    override val componentType get() = ColliderVehicleBody::class
    override val key = SokolAPI.key("collider_vehicle_body")
    val Type = ComponentType.singletonComponent(key, this)
}

interface SokolPhysicsObject : TrackedPhysicsObject {
    var entity: SokolEntity
}

@All(Collider::class, PositionRead::class)
@One(ColliderRigidBody::class)
@After(PositionAccessTarget::class)
class ColliderSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mPositionRead = ids.mapper<PositionRead>()
    private val mColliderInstance = ids.mapper<ColliderInstance>()
    private val mColliderRigidBody = ids.mapper<ColliderRigidBody>()
    private val mVehicleBodyCollider = ids.mapper<ColliderVehicleBody>()

    object Create : SokolEvent

    object CreatePhysics : SokolEvent

    data class PrePhysicsStep(
        val space: ServerPhysicsSpace
    ) : SokolEvent

    data class PostPhysicsStep(
        val space: ServerPhysicsSpace
    ) : SokolEvent

    data class Contact(
        val thisBody: PhysicsCollisionObject,
        val otherBody: PhysicsCollisionObject,
        val point: ContactManifoldPoint
    ) : SokolEvent

    @Subscribe
    fun on(event: Create, entity: SokolEntity) {
        val positionRead = mPositionRead.get(entity)
        val colliderRigidBody = mColliderRigidBody.getOr(entity)?.profile

        val bodyId = UUID.randomUUID()

        CraftBulletAPI.executePhysics {
            fun SokolPhysicsObject.preStepInternal(space: ServerPhysicsSpace) {
                this.entity.callSingle(PrePhysicsStep(space))
            }

            fun SokolPhysicsObject.postStepInternal(space: ServerPhysicsSpace) {
                this.entity.callSingle(PostPhysicsStep(space))
            }

            val physObj: SokolPhysicsObject = when {
                colliderRigidBody != null -> {
                    val shape = collisionOf(colliderRigidBody.shape)
                    val mass = colliderRigidBody.mass

                    if (mVehicleBodyCollider.has(entity)) object : PhysicsVehicle(shape, mass), SokolPhysicsObject {
                        override val id get() = bodyId
                        override val body get() = this
                        override var entity = entity

                        override fun preStep(space: ServerPhysicsSpace) {
                            preStepInternal(space)
                        }

                        override fun postStep(space: ServerPhysicsSpace) {
                            postStepInternal(space)
                        }
                    } else object : PhysicsRigidBody(shape, mass), SokolPhysicsObject {
                        override val id get() = bodyId
                        override val body get() = this
                        override var entity = entity

                        override fun preStep(space: ServerPhysicsSpace) {
                            preStepInternal(space)
                        }

                        override fun postStep(space: ServerPhysicsSpace) {
                            postStepInternal(space)
                        }
                    }
                }
                else -> throw IllegalStateException("No body type defined for collider")
            }

            val physSpace = CraftBulletAPI.spaceOf(positionRead.world)

            mColliderInstance.getOr(entity)?.let { (oldBody) ->
                physSpace.removeCollisionObject(oldBody.body)
            }
            mColliderInstance.set(entity, ColliderInstance(physObj, physSpace, null))

            val body = physObj.body
            body.transform = positionRead.transform.bullet()
            physSpace.addCollisionObject(body)

            entity.callSingle(CreatePhysics)
        }
    }
}

@All(ColliderInstance::class, Removable::class)
@Before(ColliderSystem::class)
@After(ColliderInstanceTarget::class)
class ColliderInstanceSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mColliderInstance = ids.mapper<ColliderInstance>()
    private val mRemovable = ids.mapper<Removable>()

    object Remove : SokolEvent

    @Subscribe
    fun on(event: ColliderSystem.PrePhysicsStep, entity: SokolEntity) {
        val removable = mRemovable.get(entity)

        if (removable.removed) {
            entity.callSingle(Remove)
        }
    }

    @Subscribe
    fun on(event: Remove, entity: SokolEntity) {
        val (physObj, physSpace, parentJoint) = mColliderInstance.get(entity)

        // if you don't do this, you get a segfault at btDiscreteDynamicsWorld::calculateSimulationIslands()
        parentJoint?.let { physSpace.removeJoint(it) }
        physSpace.removeCollisionObject(physObj.body)
    }

    @Subscribe
    fun on(event: ReloadEvent, entity: SokolEntity) {
        entity.callSingle(ColliderSystem.Create)
    }
}

@All(ColliderInstance::class, Collider::class, IsChild::class)
@Before(ColliderSystem::class)
@After(ColliderInstanceTarget::class)
class ColliderInstanceParentSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mColliderInstance = ids.mapper<ColliderInstance>()
    private val mIsChild = ids.mapper<IsChild>()

    private fun setJoint(entity: SokolEntity) {
        val colliderInstance = mColliderInstance.get(entity)
        val (physObj, physSpace) = colliderInstance
        val body = physObj.body
        if (body !is PhysicsRigidBody) return

        // "parent" being the closest parent which has a collider instance
        var parentEntity = mIsChild.firstParent(entity) { mColliderInstance.has(it) } ?: return
        val parentCollider = mColliderInstance.get(parentEntity)
        val parentBody = parentCollider.physObj.body as? PhysicsRigidBody ?: return

        colliderInstance.parentJoint?.let {
            physSpace.removeJoint(it)
            it.isCollisionBetweenLinkedBodies = true
        }

        /*// TODO mat rot
        val joint = New6Dof(body, parentBody,
            Vector3f.ZERO, rootLocalTransform.translation.bullet().sp(),
            Matrix3f.IDENTITY, Matrix3f.IDENTITY,
            RotationOrder.XYZ)

        repeat(6) {
            joint.set(MotorParam.LowerLimit, it, 0f)
            joint.set(MotorParam.UpperLimit, it, 0f)
        }
        joint.isCollisionBetweenLinkedBodies = false

        physSpace.addJoint(joint)
        colliderInstance.parentJoint = joint*/
    }

    @Subscribe
    fun on(event: ColliderSystem.CreatePhysics, entity: SokolEntity) {
        setJoint(entity)
    }

    @Subscribe
    fun on(event: Composite.Attach, entity: SokolEntity) {
        CraftBulletAPI.executePhysics {
            setJoint(entity)
        }
    }
}

@All(ColliderInstance::class, Collider::class, PositionWrite::class)
@Before(ColliderSystem::class)
@After(ColliderInstanceTarget::class, PositionAccessTarget::class)
class ColliderInstancePositionSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mColliderInstance = ids.mapper<ColliderInstance>()
    private val mPositionWrite = ids.mapper<PositionWrite>()

    @Subscribe
    fun on(event: ColliderSystem.PrePhysicsStep, entity: SokolEntity) {
        val colliderInstance = mColliderInstance.get(entity)
        val (physObj) = colliderInstance

        //colliderInstance.lastTransform = physObj.body.transform.alexandria()
    }

    @Subscribe
    fun on(event: ColliderSystem.PostPhysicsStep, entity: SokolEntity) {
        val colliderInstance = mColliderInstance.get(entity)
        val (physObj) = mColliderInstance.get(entity)
        val positionWrite = mPositionWrite.get(entity)

        val transform = colliderInstance.lastTransform?.let { lastTransform ->
            val delta = transformDelta(lastTransform, physObj.body.transform.alexandria())
            (positionWrite.transform * delta).also {
                positionWrite.transform = it
            }
        } ?: positionWrite.transform

        physObj.body.transform = transform.bullet()
        colliderInstance.lastTransform = transform
    }
}

@All(Collider::class, IsMob::class)
class ColliderMobSystem(ids: ComponentIdAccess) : SokolSystem {
    @Subscribe
    fun on(event: MobEvent.Spawn, entity: SokolEntity) {
        entity.callSingle(ColliderSystem.Create)
    }

    @Subscribe
    fun on(event: MobEvent.AddToWorld, entity: SokolEntity) {
        entity.callSingle(ColliderSystem.Create)
    }
}
