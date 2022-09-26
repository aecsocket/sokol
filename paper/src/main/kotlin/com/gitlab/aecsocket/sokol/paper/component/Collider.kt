package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.force
import com.gitlab.aecsocket.alexandria.core.extension.getIfExists
import com.gitlab.aecsocket.alexandria.core.keyed.Keyed
import com.gitlab.aecsocket.alexandria.core.keyed.Registry
import com.gitlab.aecsocket.alexandria.core.physics.SimpleBody
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
import com.gitlab.aecsocket.sokol.paper.util.validateStringKey
import com.jme3.bullet.objects.PhysicsRigidBody
import com.jme3.bullet.objects.PhysicsVehicle
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.NodeKey
import java.util.UUID

private const val COLLIDERS = "colliders"
private const val BACKING = "backing"
private const val MASS = "mass"
private const val DIRTY = "dirty"
private const val BODY_ID = "body_id"

data class Collider(
    var backing: Config,
    var mass: Float? = null,

    var dirty: Int = 0,
    var bodyId: UUID? = null
) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("collider")
    }

    override val componentType get() = Collider::class.java
    override val key get() = Key

    fun mass() = mass ?: backing.mass

    fun isBackingDirty() = dirty and 0x1 != 0x0
    fun markDirty() {
        dirty = Int.MAX_VALUE
    }

    override fun write(node: ConfigurationNode) {
        node.node(BACKING).set(backing.id)
        node.node(MASS).set(mass)
        node.node(DIRTY).set(dirty)
        node.node(BODY_ID).set(bodyId)
    }

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()
        .set(BACKING) { makeString(backing.id) }
        .setOrClear(MASS) { mass?.let { makeFloat(it) } }
        .set(DIRTY) { makeInt(dirty) }
        .setOrClear(BODY_ID) { bodyId?.let { makeUUID(it) } }

    @ConfigSerializable
    data class Config(
        @NodeKey override val id: String,
        val shape: List<SimpleBody>,
        val mass: Float = 1f,
    ) : Keyed

    class Type : PersistentComponentType {
        override val key get() = Key

        val registry = Registry.create<Config>()

        fun entry(id: String) = registry[id]
            ?: throw IllegalArgumentException("Invalid Collider config '$id'")

        fun load(node: ConfigurationNode) {
            node.node(COLLIDERS).childrenMap().forEach { (_, child) ->
                validateStringKey(Config::class.java, child)
                registry.register(child.force())
            }
        }

        override fun read(tag: NBTTag) = tag.asCompound().run {
            Collider(entry(get(BACKING) { asString() }),
                getOr(MASS) { asFloat() },
                get(DIRTY) { asInt() },
                getOr(BODY_ID) { asUUID() },
            )
        }

        override fun read(node: ConfigurationNode): Collider {
            return Collider(entry(node.node(BACKING).force()),
                node.node(MASS).getIfExists(),
                node.node(DIRTY).get { 0 },
                node.node(BODY_ID).getIfExists())
        }

        override fun readFactory(node: ConfigurationNode): PersistentComponentFactory {
            val backing = entry(node.force())
            return PersistentComponentFactory { Collider(backing) }
        }
    }
}

class RigidBody : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("rigid_body")
    }

    override val componentType get() = RigidBody::class.java
    override val key get() = Key

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()

    override fun write(node: ConfigurationNode) {}

    object Type : PersistentComponentType {
        override val key get() = Key

        override fun read(tag: NBTTag) = RigidBody()

        override fun read(node: ConfigurationNode) = RigidBody()

        override fun readFactory(node: ConfigurationNode) = PersistentComponentFactory { RigidBody() }
    }
}

class VehicleBody : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("vehicle_body")
    }

    override val componentType get() = VehicleBody::class.java
    override val key get() = Key

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()

    override fun write(node: ConfigurationNode) {}

    object Type : PersistentComponentType {
        override val key get() = Key

        override fun read(tag: NBTTag) = VehicleBody()

        override fun read(node: ConfigurationNode) = VehicleBody()

        override fun readFactory(node: ConfigurationNode) = PersistentComponentFactory { VehicleBody() }
    }
}

interface SokolPhysicsObject : TrackedPhysicsObject {
    var entity: SokolEntityAccess
}

@All(Position::class, Collider::class, IsValidSupplier::class)
@One(RigidBody::class, VehicleBody::class)
@Priority(PRIORITY_EARLY)
class ColliderSystem(engine: SokolEngine) : SokolSystem {
    private val mPosition = engine.componentMapper<Position>()
    private val mCollider = engine.componentMapper<Collider>()
    private val mIsValidSupplier = engine.componentMapper<IsValidSupplier>()

    private val mRigidBody = engine.componentMapper<RigidBody>()
    private val mVehicleBody = engine.componentMapper<VehicleBody>()

    @Subscribe
    fun on(event: SokolEvent.Add, entity: SokolEntityAccess) {
        val location = mPosition.map(entity)
        val collider = mCollider.map(entity)
        val isValid = mIsValidSupplier.map(entity).valid

        val shape = collisionOf(collider.backing.shape)
        val mass = collider.mass()

        val id = UUID.randomUUID()
        val physSpace = CraftBulletAPI.spaceOf(location.world)

        val transform = location.transform
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
                        entity.call(SokolEvent.PhysicsUpdate(this))
                    }
                }
                0x2 -> object : PhysicsVehicle(shape, mass), SokolPhysicsObject {
                    override val id get() = id
                    override val body get() = this
                    override var entity = entity

                    override fun update(ctx: TrackedPhysicsObject.Context) {
                        if (!isValid())
                            ctx.remove()
                        entity.call(SokolEvent.PhysicsUpdate(this))
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
    fun on(event: SokolEvent.Reload, entity: SokolEntityAccess) {
        val collider = mCollider.map(entity)
        collider.markDirty()
    }

    @Subscribe
    fun on(event: SokolEvent.Update, entity: SokolEntityAccess) {
        val location = mPosition.map(entity)
        val collider = mCollider.map(entity)

        collider.bodyId?.let { bodyId ->
            val physSpace = CraftBulletAPI.spaceOf(location.world)
            physSpace.trackedBy(bodyId)?.let { tracked ->
                val body = tracked.body
                if (tracked !is SokolPhysicsObject)
                    throw IllegalStateException("Collider physics body is not of type SokolPhysicsObject")

                if (collider.isBackingDirty())
                    body.collisionShape = collisionOf(collider.backing.shape)
                if (body is PhysicsRigidBody)
                    body.mass = collider.mass()
                collider.dirty = 0

                tracked.entity = entity
                location.transform = Transform(
                    body.physPosition.alexandria(),
                    body.physRotation.alexandria(),
                )
            } ?: run {
                collider.bodyId = null
            }
        }
    }

    @Subscribe
    fun on(event: SokolEvent.Remove, entity: SokolEntityAccess) {
        val location = mPosition.map(entity)
        val collider = mCollider.map(entity)

        collider.bodyId?.let { bodyId ->
            val physSpace = CraftBulletAPI.spaceOf(location.world)
            CraftBulletAPI.executePhysics {
                physSpace.removeTracked(bodyId)
            }

            collider.bodyId = null
        }
    }
}
