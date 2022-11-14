package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Shape
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.craftbullet.core.ServerPhysicsSpace
import com.gitlab.aecsocket.craftbullet.core.TrackedPhysicsObject
import com.gitlab.aecsocket.craftbullet.core.transform
import com.gitlab.aecsocket.craftbullet.paper.CraftBulletAPI
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.extension.alexandria
import com.gitlab.aecsocket.sokol.core.extension.bullet
import com.gitlab.aecsocket.sokol.core.extension.collisionOf
import com.gitlab.aecsocket.sokol.paper.MobEvent
import com.gitlab.aecsocket.sokol.paper.ReloadEvent
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import com.gitlab.aecsocket.sokol.paper.UpdateEvent
import com.jme3.bullet.collision.PhysicsCollisionObject
import com.jme3.bullet.objects.PhysicsGhostObject
import com.jme3.bullet.objects.PhysicsRigidBody
import com.jme3.bullet.objects.PhysicsVehicle
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
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

    data class BodyInstance(
        val body: SokolPhysicsObject,
        val physSpace: ServerPhysicsSpace
    )

    override val componentType get() = Collider::class
    override val key get() = Key

    override val dirty get() = dBodyId.dirty
    var bodyId by dBodyId

    constructor(
        bodyId: UUID?
    ) : this(Delta(bodyId))

    var body: BodyInstance? = null

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
            val bodyId = node.node(BODY_ID).get<UUID>()

            return ComponentBlueprint { Collider(bodyId) }
        }

        override fun createEmpty() = ComponentBlueprint { Collider(null) }
    }
}

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

@All(Collider::class, PositionRead::class, Removable::class)
@One(RigidBodyCollider::class, GhostBodyCollider::class)
class ColliderSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mCollider = ids.mapper<Collider>()
    private val mPositionRead = ids.mapper<PositionRead>()
    private val mRemovable = ids.mapper<Removable>()
    private val mRigidBodyCollider = ids.mapper<RigidBodyCollider>()
    private val mGhostBodyCollider = ids.mapper<GhostBodyCollider>()
    private val mVehicleBodyCollider = ids.mapper<VehicleBodyCollider>()

    object Create : SokolEvent

    object Remove : SokolEvent

    object Rebuild : SokolEvent

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        val collider = mCollider.get(entity)
        val positionRead = mPositionRead.get(entity)

        val bodyId = collider.bodyId ?: return
        val physSpace = CraftBulletAPI.spaceOf(positionRead.world)
        val body = physSpace.trackedBy(bodyId) as? SokolPhysicsObject ?: return

        collider.body = Collider.BodyInstance(body, physSpace)
    }

    @Subscribe
    fun on(event: Create, entity: SokolEntity) {
        val collider = mCollider.get(entity)
        val removable = mRemovable.get(entity)
        val positionRead = mPositionRead.get(entity)
        val rigidBodyCollider = mRigidBodyCollider.getOr(entity)?.profile
        val ghostBodyCollider = mGhostBodyCollider.getOr(entity)?.profile

        val bodyId = UUID.randomUUID()
        collider.bodyId = bodyId

        CraftBulletAPI.executePhysics {
            val body: PhysicsCollisionObject = when {
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

            body.transform = positionRead.transform.bullet()
            CraftBulletAPI.spaceOf(positionRead.world).addCollisionObject(body)
        }
    }

    @Subscribe
    fun on(event: Remove, entity: SokolEntity) {
        val collider = mCollider.get(entity)
        val (body, physSpace) = collider.body ?: return

        collider.bodyId = null

        CraftBulletAPI.executePhysics {
            physSpace.removeCollisionObject(body.body)
        }
    }

    @Subscribe
    fun on(event: UpdateEvent, entity: SokolEntity) {
        val collider = mCollider.get(entity)
        val (body) = collider.body ?: return

        body.entity = entity
    }

    @Subscribe
    fun on(event: Rebuild, entity: SokolEntity) {
        val collider = mCollider.get(entity)
        val rigidBodyCollider = mRigidBodyCollider.getOr(entity)?.profile
        val (physObj) = collider.body ?: return
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
    fun on(event: ReloadEvent, entity: SokolEntity) {
        entity.callSingle(Rebuild)
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

    @Subscribe
    fun on(event: MobEvent.RemoveFromWorld, entity: SokolEntity) {
        entity.callSingle(ColliderSystem.Remove)
    }
}

@All(Collider::class, PositionWrite::class)
@After(ColliderSystem::class)
class ColliderPositionSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mCollider = ids.mapper<Collider>()
    private val mPositionWrite = ids.mapper<PositionWrite>()

    @Subscribe
    fun on(event: UpdateEvent, entity: SokolEntity) {
        val collider = mCollider.get(entity)
        val positionWrite = mPositionWrite.get(entity)
        val (physObj) = collider.body ?: return
        val body = physObj.body

        positionWrite.transform = body.transform.alexandria()
    }
}
