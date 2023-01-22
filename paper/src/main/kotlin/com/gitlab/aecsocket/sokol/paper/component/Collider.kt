package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.matrix
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
import com.gitlab.aecsocket.sokol.paper.stat.DecimalCounterStat
import com.jme3.bullet.RotationOrder
import com.jme3.bullet.collision.PhysicsCollisionObject
import com.jme3.bullet.joints.New6Dof
import com.jme3.bullet.joints.motors.MotorParam
import com.jme3.bullet.objects.PhysicsRigidBody
import com.jme3.bullet.objects.PhysicsVehicle
import com.jme3.math.Matrix3f
import com.jme3.math.Vector3f
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required
import org.spongepowered.configurate.objectmapping.meta.Setting
import java.util.UUID

interface SokolPhysicsObject : TrackedPhysicsObject {
    var entity: SokolEntity
}

object Collider : SimplePersistentComponent {
    override val componentType get() = Collider::class
    override val key = SokolAPI.key("collider")
    val Type = ComponentType.singletonComponent(key, this)

    fun init(ctx: Sokol.InitContext) {
        ctx.persistentComponent(Type)
        ctx.transientComponent<ColliderInstance>()
        ctx.persistentComponent(ColliderRigidBody.Type)
        ctx.persistentComponent(ColliderVehicleBody.Type)
        ctx.system { ColliderInstanceTarget }
        ctx.system { ColliderSystem(it) }
        ctx.system { ColliderInstanceSystem(it) }
        ctx.system { ColliderInstanceRemovableSystem(it) }
        ctx.system { ColliderInstanceParentSystem(it) }
        ctx.system { ColliderMobSystem(ctx.sokol, it) }

        ctx.sokol.components.stats.apply {
            stats(ColliderRigidBody.Stats.All)
        }
    }
}

data class ColliderInstance(
    val physObj: SokolPhysicsObject,
    val physSpace: ServerPhysicsSpace,
    var parentJoint: New6Dof?
) : SokolComponent {
    override val componentType get() = ColliderInstance::class

    var lastTransform: Transform? = null
    var treeIgnored: List<PhysicsCollisionObject> = emptyList()
}

data class ColliderRigidBody(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("collider_rigid_body")
        val Type = ComponentType.deserializing(Key, Profile::class)
    }

    object Stats {
        val Mass = DecimalCounterStat(Key.with("mass"))
        val All = listOf(Mass)
    }

    override val componentType get() = ColliderRigidBody::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        @Required @Setting(nodeFromParent = true) val shape: Shape
    ) : SimpleComponentProfile<ColliderRigidBody> {
        override val componentType get() = ColliderRigidBody::class

        override fun createEmpty() = ComponentBlueprint { ColliderRigidBody(this) }
    }
}

object ColliderVehicleBody : SimplePersistentComponent {
    override val componentType get() = ColliderVehicleBody::class
    override val key = SokolAPI.key("collider_vehicle_body")
    val Type = ComponentType.singletonComponent(key, this)
}

object ColliderInstanceTarget : SokolSystem

@All(Collider::class, PositionAccess::class, Stats::class)
@One(ColliderRigidBody::class)
@After(PositionAccessTarget::class)
class ColliderSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mPositionAccess = ids.mapper<PositionAccess>()
    private val mColliderInstance = ids.mapper<ColliderInstance>()
    private val mColliderRigidBody = ids.mapper<ColliderRigidBody>()
    private val mVehicleBodyCollider = ids.mapper<ColliderVehicleBody>()
    private val mStatsInstance = ids.mapper<StatsInstance>()

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
        val positionRead = mPositionAccess.get(entity)
        val colliderRigidBody = mColliderRigidBody.getOr(entity)?.profile
        val stats = mStatsInstance.statMap(entity)

        val bodyId = UUID.randomUUID()

        CraftBulletAPI.executePhysics {
            fun SokolPhysicsObject.preStepInternal(space: ServerPhysicsSpace) {
                this.entity.call(PrePhysicsStep(space))
            }

            fun SokolPhysicsObject.postStepInternal(space: ServerPhysicsSpace) {
                this.entity.call(PostPhysicsStep(space))
            }

            val physObj: SokolPhysicsObject = when {
                colliderRigidBody != null -> {
                    val shape = colliderRigidBody.shape.bullet()
                    val mass = stats.value(ColliderRigidBody.Stats.Mass).toFloat()

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

            entity.call(CreatePhysics)
        }
    }
}

@All(ColliderInstance::class, PositionAccess::class)
@Before(ColliderSystem::class)
@After(ColliderInstanceTarget::class)
class ColliderInstanceSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mColliderInstance = ids.mapper<ColliderInstance>()
    private val mPositionAccess = ids.mapper<PositionAccess>()

    object Remove : SokolEvent

    data class ChangeContactResponse(
        val hasResponse: Boolean
    ) : SokolEvent

    @Subscribe
    fun on(event: ColliderSystem.PostPhysicsStep, entity: SokolEntity) {
        val colliderInstance = mColliderInstance.get(entity)
        val (physObj) = colliderInstance
        val positionAccess = mPositionAccess.get(entity)

        val transform = colliderInstance.lastTransform?.let { lastTransform ->
            val delta = transformDelta(lastTransform, physObj.body.transform.alexandria())
            (positionAccess.transform * delta).also {
                positionAccess.transform = it
            }
        } ?: positionAccess.transform

        physObj.body.transform = transform.bullet()
        colliderInstance.lastTransform = transform
    }

    @Subscribe
    fun on(event: ChangeContactResponse, entity: SokolEntity) {
        val (physObj) = mColliderInstance.get(entity)
        val body = physObj.body as? PhysicsRigidBody ?: return

        body.isContactResponse = event.hasResponse
    }

    @Subscribe
    fun on(event: Remove, entity: SokolEntity) {
        val (physObj, physSpace, parentJoint) = mColliderInstance.get(entity)

        // if you don't do this, you get a segfault at btDiscreteDynamicsWorld::calculateSimulationIslands()
        parentJoint?.let { physSpace.removeJoint(it) }
        physSpace.removeCollisionObject(physObj.body)
    }
}

@All(ColliderInstance::class, Removable::class)
@After(ColliderInstanceTarget::class)
class ColliderInstanceRemovableSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mRemovable = ids.mapper<Removable>()

    @Subscribe
    fun on(event: ColliderSystem.PrePhysicsStep, entity: SokolEntity) {
        val removable = mRemovable.get(entity)

        if (removable.removed) {
            entity.call(ColliderInstanceSystem.Remove)
        }
    }
}

@All(ColliderInstance::class, Collider::class, IsChild::class, PositionAccess::class)
@Before(ColliderSystem::class)
@After(ColliderInstanceTarget::class, PositionAccessTarget::class)
class ColliderInstanceParentSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mColliderInstance = ids.mapper<ColliderInstance>()
    private val mIsChild = ids.mapper<IsChild>()
    private val mPositionAccess = ids.mapper<PositionAccess>()
    private val mComposite = ids.mapper<Composite>()

    private fun setJoint(entity: SokolEntity) {
        val colliderInstance = mColliderInstance.get(entity)
        val (physObj, physSpace) = colliderInstance
        val body = physObj.body
        if (body !is PhysicsRigidBody) return
        val positionAccess = mPositionAccess.get(entity)

        // "parent" being the closest parent which has a collider instance
        mIsChild.firstParent(entity) {
            mColliderInstance.has(it) && mPositionAccess.has(it)
        }?.let { parentEntity ->
            // joint between this entity and its parent
            val pColliderInstance = mColliderInstance.get(parentEntity)
            val pPositionAccess = mPositionAccess.get(parentEntity)
            val parentBody = pColliderInstance.physObj.body as? PhysicsRigidBody ?: return@let

            // remove old joint settings
            colliderInstance.treeIgnored.forEach { body.removeFromIgnoreList(it) }
            colliderInstance.treeIgnored = emptyList()
            colliderInstance.parentJoint?.let { physSpace.removeJoint(it) }

            val delta = transformDelta(pPositionAccess.transform, positionAccess.transform)
            val joint = New6Dof(body, parentBody,
                Vector3f.ZERO, delta.position.bullet().sp(),
                Matrix3f.IDENTITY, delta.rotation.matrix().bulletSp(), // todo
                RotationOrder.XYZ)

            repeat(6) {
                joint.set(MotorParam.LowerLimit, it, 0f)
                joint.set(MotorParam.UpperLimit, it, 0f)
            }

            physSpace.addJoint(joint)
            colliderInstance.parentJoint = joint
        }

        // ignore all bodies on the same tree
        /*val treeIgnored = mComposite.all(mIsChild.root(entity)).mapNotNull { child ->
            val cBody = mColliderInstance.getOr(child)?.physObj?.body ?: return@mapNotNull null
            if (body === cBody) return@mapNotNull null
            body.addToIgnoreList(cBody)
            cBody
        }
        colliderInstance.treeIgnored = treeIgnored*/
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

    @Subscribe
    fun on(event: Composite.Detach, entity: SokolEntity) {
        CraftBulletAPI.executePhysics {

        }
    }
}

@All(IsMob::class)
class ColliderMobSystem(
    private val sokol: Sokol,
    ids: ComponentIdAccess
) : SokolSystem {
    private val mIsMob = ids.mapper<IsMob>()
    private val mComposite = ids.mapper<Composite>()

    @Subscribe
    fun on(event: MobEvent.Spawn, entity: SokolEntity) {
        mComposite.forwardAll(entity, ColliderSystem.Create)
    }

    @Subscribe
    fun on(event: MobEvent.AddToWorld, entity: SokolEntity) {
        mComposite.forwardAll(entity, ColliderSystem.Create)
    }

    @Subscribe
    fun on(event: ReloadEvent, entity: SokolEntity) {
        val mob = mIsMob.get(entity).mob
        val oldEntity = sokol.resolver.mobTrackedBy(mob) ?: return
        CraftBulletAPI.executePhysics {
            mComposite.forwardAll(oldEntity, ColliderInstanceSystem.Remove)
            mComposite.forwardAll(entity, ColliderSystem.Create)
        }
    }
}
