package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.getIfExists
import com.gitlab.aecsocket.alexandria.core.physics.Shape
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.craftbullet.core.*
import com.gitlab.aecsocket.craftbullet.paper.CraftBulletAPI
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.extension.*
import com.gitlab.aecsocket.sokol.paper.MobEvent
import com.gitlab.aecsocket.sokol.paper.ReloadEvent
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import com.gitlab.aecsocket.sokol.paper.UpdateEvent
import com.jme3.bullet.RotationOrder
import com.jme3.bullet.joints.New6Dof
import com.jme3.bullet.joints.motors.MotorParam
import com.jme3.bullet.objects.PhysicsGhostObject
import com.jme3.bullet.objects.PhysicsRigidBody
import com.jme3.bullet.objects.PhysicsVehicle
import com.jme3.math.Matrix3f
import com.jme3.math.Vector3f
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required
import java.util.UUID

private const val BODY_ID = "body_id"

data class Collider(
    val dBodyId: Delta<UUID?>
) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("collider")
        val Type = ComponentType.singletonProfile(Key, Profile)
    }

    override val componentType get() = Collider::class
    override val key get() = Key

    override val dirty get() = dBodyId.dirty
    var bodyId by dBodyId

    constructor(
        bodyId: UUID?
    ) : this(Delta(bodyId))

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()
        .setOrClear(BODY_ID) { bodyId?.let { makeUUID(it) } }

    override fun writeDelta(tag: NBTTag): NBTTag {
        val compound = tag.asCompound()

        dBodyId.ifDirty { compound.setOrClear(BODY_ID) { it?.let { makeUUID(it) } } }

        return compound
    }

    override fun serialize(node: ConfigurationNode) {
        node.node(BODY_ID).set(bodyId)
    }

    object Profile : ComponentProfile {
        override val componentType get() = Collider::class

        override fun read(tag: NBTTag) = tag.asCompound { compound ->
            val bodyId = compound.getOr(BODY_ID) { asUUID() }

            ComponentBlueprint { Collider(bodyId) }
        }

        override fun deserialize(node: ConfigurationNode): ComponentBlueprint<Collider> {
            val bodyId = node.node(BODY_ID).getIfExists<UUID>()

            return ComponentBlueprint { Collider(bodyId) }
        }

        override fun createEmpty() = ComponentBlueprint { Collider(null) }
    }
}

data class ColliderInstance(
    val body: SokolPhysicsObject,
    val physSpace: ServerPhysicsSpace
) : SokolComponent {
    override val componentType get() = ColliderInstance::class
}

object ColliderInstanceTarget : SokolSystem

// TODO mass could probably be moved to the stats system once that's done
data class RigidBodyCollider(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("rigid_body_collider")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = RigidBodyCollider::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        @Required val shape: Shape,
        val mass: Float = 1f
    ) : SimpleComponentProfile {
        override val componentType get() = RigidBodyCollider::class

        override fun createEmpty() = ComponentBlueprint { RigidBodyCollider(this) }
    }
}

object VehicleBodyCollider : SimplePersistentComponent {
    override val componentType get() = VehicleBodyCollider::class
    override val key = SokolAPI.key("vehicle_body_collider")
    val Type = ComponentType.singletonComponent(key, this)
}

data class GhostBodyCollider(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("ghost_body_collider")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = GhostBodyCollider::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        @Required val shape: Shape
    ) : SimpleComponentProfile {
        override val componentType get() = GhostBodyCollider::class

        override fun createEmpty() = ComponentBlueprint { GhostBodyCollider(this) }
    }
}

interface SokolPhysicsObject : TrackedPhysicsObject {
    var entity: SokolEntity
}

@All(Collider::class, PositionRead::class)
@None(ColliderInstance::class)
@Before(ColliderInstanceTarget::class)
@After(PositionPreTarget::class)
class ColliderConstructSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mCollider = ids.mapper<Collider>()
    private val mPositionRead = ids.mapper<PositionRead>()
    private val mColliderInstance = ids.mapper<ColliderInstance>()

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        val collider = mCollider.get(entity)
        val positionRead = mPositionRead.get(entity)

        val bodyId = collider.bodyId ?: return
        val physSpace = CraftBulletAPI.spaceOf(positionRead.world)
        val body = physSpace.trackedBy(bodyId) as? SokolPhysicsObject ?: return

        mColliderInstance.set(entity, ColliderInstance(body, physSpace))
    }
}

@All(Collider::class, PositionRead::class, Removable::class)
@One(RigidBodyCollider::class, GhostBodyCollider::class)
@None(ColliderInstance::class)
@After(PositionTarget::class, RemovableTarget::class)
class ColliderSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mCollider = ids.mapper<Collider>()
    private val mPositionRead = ids.mapper<PositionRead>()
    private val mRemovable = ids.mapper<Removable>()
    private val mColliderInstance = ids.mapper<ColliderInstance>()
    private val mRigidBodyCollider = ids.mapper<RigidBodyCollider>()
    private val mGhostBodyCollider = ids.mapper<GhostBodyCollider>()
    private val mVehicleBodyCollider = ids.mapper<VehicleBodyCollider>()
    private val mIsChild = ids.mapper<IsChild>()

    object Create : SokolEvent

    @Subscribe
    fun on(event: Create, entity: SokolEntity) {
        val collider = mCollider.get(entity)
        val removable = mRemovable.get(entity)
        val positionRead = mPositionRead.get(entity)
        val rigidBodyCollider = mRigidBodyCollider.getOr(entity)?.profile
        val ghostBodyCollider = mGhostBodyCollider.getOr(entity)?.profile
        val parent = mIsChild.getOr(entity)?.parent

        val bodyId = UUID.randomUUID()
        collider.bodyId = bodyId

        CraftBulletAPI.executePhysics {
            val physObj: SokolPhysicsObject = when {
                rigidBodyCollider != null -> {
                    val shape = collisionOf(rigidBodyCollider.shape)
                    val mass = rigidBodyCollider.mass

                    if (mVehicleBodyCollider.has(entity)) object : PhysicsVehicle(shape, mass), SokolPhysicsObject {
                        override val id get() = bodyId
                        override val body get() = this
                        override var entity = entity

                        override fun update(ctx: TrackedPhysicsObject.Context) {
                            if (removable.removed) ctx.remove()
                        }
                    } else object : PhysicsRigidBody(shape, mass), SokolPhysicsObject {
                        override val id get() = bodyId
                        override val body get() = this
                        override var entity = entity

                        override fun update(ctx: TrackedPhysicsObject.Context) {
                            if (removable.removed) ctx.remove()
                        }
                    }
                }
                ghostBodyCollider != null -> {
                    val shape = collisionOf(ghostBodyCollider.shape)

                    object : PhysicsGhostObject(shape), SokolPhysicsObject {
                        override val id get() = bodyId
                        override val body get() = this
                        override var entity = entity

                        override fun update(ctx: TrackedPhysicsObject.Context) {
                            if (removable.removed) ctx.remove()
                        }
                    }
                }
                else -> throw IllegalStateException("No body type defined for collider")
            }

            val physSpace = CraftBulletAPI.spaceOf(positionRead.world)
            mColliderInstance.set(entity, ColliderInstance(physObj, physSpace))

            val body = physObj.body
            body.transform = positionRead.transform.bullet()
            physSpace.addCollisionObject(body)

            if (body is PhysicsRigidBody && parent != null) {
                (mColliderInstance.getOr(parent)?.body?.body as? PhysicsRigidBody)?.let { pBody ->
                    val joint = New6Dof(body, pBody,
                        Vector3f(0f, -0.5f, 0f), Vector3f(0f, 0.5f, 0f),
                        Matrix3f.IDENTITY, Matrix3f.IDENTITY,
                        RotationOrder.XYZ)

                    repeat(6) {
                        joint.set(MotorParam.LowerLimit, it, 0f)
                        joint.set(MotorParam.UpperLimit, it, 0f)
                    }

                    physSpace.addJoint(joint)

                    body.addToIgnoreList(pBody)
                }
            }
        }
    }
}

@All(ColliderInstance::class, Collider::class)
@Before(ColliderSystem::class)
@After(ColliderInstanceTarget::class)
class ColliderInstanceSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mCollider = ids.mapper<Collider>()
    private val mColliderInstance = ids.mapper<ColliderInstance>()
    private val mRigidBodyCollider = ids.mapper<RigidBodyCollider>()

    object Remove : SokolEvent

    object Rebuild : SokolEvent

    @Subscribe
    fun on(event: Remove, entity: SokolEntity) {
        val (physObj, physSpace) = mColliderInstance.get(entity)
        val collider = mCollider.get(entity)

        collider.bodyId = null

        CraftBulletAPI.executePhysics {
            physSpace.removeCollisionObject(physObj.body)
        }
    }

    @Subscribe
    fun on(event: Rebuild, entity: SokolEntity) {
        val (physObj) = mColliderInstance.get(entity)
        val rigidBodyCollider = mRigidBodyCollider.getOr(entity)?.profile
        val body = physObj.body

        CraftBulletAPI.executePhysics {
            rigidBodyCollider?.let {
                body.collisionShape = collisionOf(rigidBodyCollider.shape)
                if (body is PhysicsRigidBody) {
                    body.mass = rigidBodyCollider.mass
                }
            }
        }
    }

    @Subscribe
    fun on(event: UpdateEvent, entity: SokolEntity) {
        val (physObj) = mColliderInstance.get(entity)

        physObj.entity = entity
    }

    @Subscribe
    fun on(event: ReloadEvent, entity: SokolEntity) {
        entity.callSingle(Rebuild)
    }
}

@All(ColliderInstance::class, Collider::class, PositionWrite::class)
@Before(ColliderSystem::class)
@After(ColliderInstanceTarget::class, PositionTarget::class)
class ColliderInstancePositionSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mColliderInstance = ids.mapper<ColliderInstance>()
    private val mPositionWrite = ids.mapper<PositionWrite>()

    @Subscribe
    fun on(event: UpdateEvent, entity: SokolEntity) {
        val (physObj) = mColliderInstance.get(entity)
        val positionWrite = mPositionWrite.get(entity)

        positionWrite.transform = physObj.body.transform.alexandria()
    }
}

@All(Collider::class, PositionRead::class)
@After(PositionTarget::class, RemovableTarget::class)
class ColliderMobSystem(ids: ComponentIdAccess) : SokolSystem {
    @Subscribe
    fun on(event: MobEvent.Spawn, entity: SokolEntity) {
        entity.callSingle(ColliderSystem.Create)
    }

    @Subscribe
    fun on(event: MobEvent.AddToWorld, entity: SokolEntity) {
        entity.callSingle(ColliderSystem.Create)
    }

    @Subscribe
    fun on(event: MobEvent.RemoveFromWorld, entity: SokolEntity) {
        entity.callSingle(ColliderInstanceSystem.Remove)
    }
}
