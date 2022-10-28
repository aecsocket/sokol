package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.getIfExists
import com.gitlab.aecsocket.alexandria.core.physics.Shape
import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.core.physics.Vector3
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.craftbullet.core.TrackedPhysicsObject
import com.gitlab.aecsocket.craftbullet.core.addShape
import com.gitlab.aecsocket.craftbullet.core.physPosition
import com.gitlab.aecsocket.craftbullet.core.physRotation
import com.gitlab.aecsocket.craftbullet.paper.CraftBulletAPI
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.extension.*
import com.gitlab.aecsocket.sokol.paper.*
import com.jme3.bullet.collision.shapes.CollisionShape
import com.jme3.bullet.collision.shapes.CompoundCollisionShape
import com.jme3.bullet.objects.PhysicsRigidBody
import com.jme3.bullet.objects.PhysicsVehicle
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required
import java.util.UUID

private const val MASS = "mass"
private const val CENTER_OF_MASS = "center_of_mass"
private const val BODY_ID = "body_id"

data class Collider(
    val profile: Profile,
    var mass: Float?,
    var centerOfMass: Vector3,
    var bodyId: UUID?,
) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("collider")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = Collider::class
    override val key get() = Key

    fun mass() = mass ?: profile.mass

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()
        .setOrClear(MASS) { mass?.let { makeFloat(it) } }
        .set(CENTER_OF_MASS) { makeVector3(centerOfMass) }
        .setOrClear(BODY_ID) { bodyId?.let { makeUUID(it) } }

    override fun write(node: ConfigurationNode) {
        node.node(MASS).set(mass)
        node.node(CENTER_OF_MASS).set(centerOfMass)
        node.node(BODY_ID).set(bodyId)
    }

    @ConfigSerializable
    data class Profile(
        @Required val shape: Shape,
        val mass: Float = 1f
    ) : ComponentProfile {
        override fun read(tag: NBTTag) = tag.asCompound().run { Collider(this@Profile,
            getOr(MASS) { asFloat() },
            getOr(CENTER_OF_MASS) { asVector3() } ?: Vector3.Zero,
            getOr(BODY_ID) { asUUID() },
        ) }

        override fun read(node: ConfigurationNode) = Collider(this,
            node.node(MASS).getIfExists(),
            node.node(CENTER_OF_MASS).get { Vector3.Zero },
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

@All(Collider::class, CompositeTransform::class)
@After(CompositeTransformSystem::class)
class ColliderBuildSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mCollider = mappers.componentMapper<Collider>()
    private val mCompositeTransform = mappers.componentMapper<CompositeTransform>()
    private val mComposite = mappers.componentMapper<Composite>()

    @Subscribe
    fun on(event: BuildBody, entity: SokolEntity) {
        val collider = mCollider.get(entity)
        val compositeTransform = mCompositeTransform.get(entity)

        event.addBody(
            collisionOf(collider.profile.shape),
            compositeTransform.transform,
            collider.mass()
        )

        mComposite.forward(entity, event)
    }

    data class BodyData(
        val shape: CollisionShape,
        val transform: Transform,
        val mass: Float,
    )

    data class BuildBody(
        val bodies: MutableList<BodyData> = ArrayList()
    ) : SokolEvent {
        fun addBody(shape: CollisionShape, transform: Transform, mass: Float) {
            if (mass < 0)
                throw IllegalArgumentException("Cannot have negative mass")
            bodies.add(BodyData(shape, transform, mass))
        }
    }
}

@All(Collider::class, PositionWrite::class, IsValidSupplier::class)
@One(RigidBody::class, VehicleBody::class)
class ColliderSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mPosition = mappers.componentMapper<PositionWrite>()
    private val mCollider = mappers.componentMapper<Collider>()
    private val mIsValidSupplier = mappers.componentMapper<IsValidSupplier>()
    private val mRigidBody = mappers.componentMapper<RigidBody>()
    private val mVehicleBody = mappers.componentMapper<VehicleBody>()

    private data class BodyData(
        val shape: CollisionShape,
        val mass: Float,
        val centerOfMass: Vector3
    )

    private fun buildBody(entity: SokolEntity): BodyData {
        val (bodies) = entity.call(ColliderBuildSystem.BuildBody())

        val totalMass = bodies.map { it.mass }.sum()
        var centerOfMass = Vector3.Zero
        bodies.forEach { (_, transform, mass) ->
            val portionOfTotal = mass / totalMass
            centerOfMass += transform.translation * portionOfTotal.toDouble()
        }

        val compoundShape = CompoundCollisionShape()
        bodies.forEach { (shape, transform) ->
            val newTransform = Transform(
                transform.translation - centerOfMass,
                transform.rotation
            )
            compoundShape.addShape(shape, newTransform.bullet())
        }

        return BodyData(compoundShape, totalMass, centerOfMass)
    }

    @Subscribe
    fun on(event: RebuildBody, entity: SokolEntity) {
        val position = mPosition.get(entity)
        val collider = mCollider.get(entity)

        val physSpace = CraftBulletAPI.spaceOf(position.world)
        val tracked = physSpace.trackedBy(collider.bodyId ?: return) ?: return
        val body = tracked.body

        val (shape, mass, centerOfMass) = buildBody(entity)

        CraftBulletAPI.executePhysics {
            body.collisionShape = shape
            if (body is PhysicsRigidBody)
                body.mass = mass
        }

        collider.centerOfMass = centerOfMass
    }

    @Subscribe
    fun on(event: SokolEvent.Add, entity: SokolEntity) {
        val position = mPosition.get(entity)
        val collider = mCollider.get(entity)
        val isValid = mIsValidSupplier.get(entity).valid

        val (shape, mass, centerOfMass) = buildBody(entity)

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

        collider.centerOfMass = centerOfMass
        collider.bodyId = id
    }

    @Subscribe
    fun on(event: SokolEvent.Reload, entity: SokolEntity) {
        entity.call(RebuildBody)
    }

    @Subscribe
    fun on(event: SokolEvent.Update, entity: SokolEntity) {
        val position = mPosition.get(entity)
        val collider = mCollider.get(entity)

        val physSpace = CraftBulletAPI.spaceOf(position.world)


        physSpace.trackedBy(collider.bodyId ?: return)?.let { tracked ->
            val body = tracked.body
            if (tracked !is SokolPhysicsObject)
                throw SystemExecutionException("Collider physics body is not of type ${SokolPhysicsObject::class}")

            tracked.entity = entity
            position.transform = Transform(
                body.physPosition.alexandria(),
                body.physRotation.alexandria(),
            ) + Transform(-collider.centerOfMass)
        } ?: run {
            collider.bodyId = null
        }
    }

    @Subscribe
    fun on(event: SokolEvent.Remove, entity: SokolEntity) {
        val position = mPosition.get(entity)
        val collider = mCollider.get(entity)

        collider.bodyId?.let { bodyId ->
            val physSpace = CraftBulletAPI.spaceOf(position.world)
            CraftBulletAPI.executePhysics {
                physSpace.removeTracked(bodyId)
            }

            collider.bodyId = null
        }
    }

    object RebuildBody : SokolEvent

    // NB: modifying component data during this will not persist
    data class PhysicsUpdate(
        val body: TrackedPhysicsObject
    ) : SokolEvent
}
