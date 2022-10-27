package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.getIfExists
import com.gitlab.aecsocket.alexandria.core.physics.Shape
import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.craftbullet.core.TrackedPhysicsObject
import com.gitlab.aecsocket.craftbullet.core.physPosition
import com.gitlab.aecsocket.craftbullet.core.physRotation
import com.gitlab.aecsocket.craftbullet.paper.CraftBulletAPI
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.extension.alexandria
import com.gitlab.aecsocket.sokol.core.extension.bullet
import com.gitlab.aecsocket.sokol.core.extension.collisionOf
import com.gitlab.aecsocket.sokol.paper.*
import com.jme3.bullet.objects.PhysicsRigidBody
import com.jme3.bullet.objects.PhysicsVehicle
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required
import java.util.UUID

private const val MASS = "mass"
private const val DIRTY = "dirty"
private const val BODY_ID = "body_id"

data class Collider(
    val profile: Profile,
    var mass: Float? = null,
    var dirty: Int = 0,
    var bodyId: UUID? = null
) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("collider")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = Collider::class
    override val key get() = Key

    fun mass() = mass ?: profile.mass

    fun isProfileDirty() = dirty and 0x1 != 0x0
    fun isMassDirty() = dirty and 0x2 != 0x0
    fun markDirty() {
        dirty = Int.MAX_VALUE
    }

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()
        .setOrClear(MASS) { mass?.let { makeFloat(it) } }
        .set(DIRTY) { makeInt(dirty) }
        .setOrClear(BODY_ID) { bodyId?.let { makeUUID(it) } }

    override fun write(node: ConfigurationNode) {
        node.node(MASS).set(mass)
        node.node(DIRTY).set(dirty)
        node.node(BODY_ID).set(bodyId)
    }

    @ConfigSerializable
    data class Profile(
        @Required val shape: Shape,
        val mass: Float = 1f
    ) : ComponentProfile {
        override fun read(tag: NBTTag) = tag.asCompound().run { Collider(this@Profile,
            getOr(MASS) { asFloat() },
            get(DIRTY) { asInt() },
            getOr(BODY_ID) { asUUID() },
        ) }

        override fun read(node: ConfigurationNode) = Collider(this,
            node.node(MASS).getIfExists(),
            node.node(DIRTY).get { 0 },
            node.node(BODY_ID).getIfExists()
        )
    }
}

object RigidBody : PersistentComponent {
    override val componentType get() = RigidBody::class
    override val key = SokolAPI.key("rigid_body")
    val Type = ComponentType.singletonComponent(key, RigidBody)

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()

    override fun write(node: ConfigurationNode) {}
}

object VehicleBody : PersistentComponent {
    override val componentType get() = VehicleBody::class
    override val key = SokolAPI.key("vehicle_body")
    val Type = ComponentType.singletonComponent(key, VehicleBody)

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()

    override fun write(node: ConfigurationNode) {}
}

interface SokolPhysicsObject : TrackedPhysicsObject {
    var entity: SokolEntity
}

@All(Collider::class, PositionWrite::class, IsValidSupplier::class)
@One(RigidBody::class, VehicleBody::class)
@Priority(PRIORITY_EARLY)
class ColliderSystem(engine: SokolEngine) : SokolSystem {
    private val mPosition = engine.componentMapper<PositionWrite>()
    private val mCollider = engine.componentMapper<Collider>()
    private val mIsValidSupplier = engine.componentMapper<IsValidSupplier>()

    private val mRigidBody = engine.componentMapper<RigidBody>()
    private val mVehicleBody = engine.componentMapper<VehicleBody>()

    @Subscribe
    fun on(event: SokolEvent.Add, entity: SokolEntity) {
        val position = mPosition.map(entity)
        val collider = mCollider.map(entity)
        val isValid = mIsValidSupplier.map(entity).valid

        val shape = collisionOf(collider.profile.shape)
        val mass = collider.mass()

        val id = UUID.randomUUID()
        val physSpace = CraftBulletAPI.spaceOf(position.world)

        val transform = position.transform
        CraftBulletAPI.executePhysics {
            val typeField =
                (if (mRigidBody.has(entity)) 0x1 else 0) or
                (if (mVehicleBody.has(entity)) 0x2 else 0)

            val body = when (typeField) {
                0x1 -> object : PhysicsRigidBody(shape, mass), SokolPhysicsObject {
                    override val id get() = id
                    override val body get() = this
                    override var entity = entity

                    override fun update(ctx: TrackedPhysicsObject.Context) {
                        if (!isValid())
                            ctx.remove()
                        entity.call(PhysicsUpdate(this))
                    }
                }
                0x2 -> object : PhysicsVehicle(shape, mass), SokolPhysicsObject {
                    override val id get() = id
                    override val body get() = this
                    override var entity = entity

                    override fun update(ctx: TrackedPhysicsObject.Context) {
                        if (!isValid())
                            ctx.remove()
                        entity.call(PhysicsUpdate(this))
                    }
                }
                else -> throw IllegalStateException("Multiple body types defined for collider")
            }

            body.physPosition = transform.translation.bullet()
            body.physRotation = transform.rotation.bullet()
            body.collisionShape

            physSpace.addCollisionObject(body)
        }

        collider.bodyId = id
    }

    @Subscribe
    fun on(event: SokolEvent.Reload, entity: SokolEntity) {
        val collider = mCollider.map(entity)
        collider.markDirty()
    }

    @Subscribe
    fun on(event: SokolEvent.Update, entity: SokolEntity) {
        val position = mPosition.map(entity)
        val collider = mCollider.map(entity)

        collider.bodyId?.let { bodyId ->
            val physSpace = CraftBulletAPI.spaceOf(position.world)
            physSpace.trackedBy(bodyId)?.let { tracked ->
                val body = tracked.body
                if (tracked !is SokolPhysicsObject)
                    throw IllegalStateException("Collider physics body is not of type ${SokolPhysicsObject::class.java}")

                val backingDirty = collider.isProfileDirty()
                val massDirty = collider.isMassDirty()
                collider.dirty = 0

                CraftBulletAPI.executePhysics {
                    if (backingDirty)
                        body.collisionShape = collisionOf(collider.profile.shape)
                    if (massDirty && body is PhysicsRigidBody)
                        body.mass = collider.mass()
                }

                tracked.entity = entity
                position.transform = Transform(
                    body.physPosition.alexandria(),
                    body.physRotation.alexandria(),
                )
            } ?: run {
                collider.bodyId = null
            }
        }
    }

    @Subscribe
    fun on(event: SokolEvent.Remove, entity: SokolEntity) {
        val position = mPosition.map(entity)
        val collider = mCollider.map(entity)

        collider.bodyId?.let { bodyId ->
            val physSpace = CraftBulletAPI.spaceOf(position.world)
            CraftBulletAPI.executePhysics {
                physSpace.removeTracked(bodyId)
            }

            collider.bodyId = null
        }
    }

    // NB: modifying component data during this will not persist
    data class PhysicsUpdate(
        val body: TrackedPhysicsObject
    ) : SokolEvent
}
