package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.getIfExists
import com.gitlab.aecsocket.alexandria.core.physics.Shape
import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.core.physics.Vector3
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.craftbullet.core.*
import com.gitlab.aecsocket.craftbullet.paper.CraftBulletAPI
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.extension.*
import com.gitlab.aecsocket.sokol.paper.*
import com.gitlab.aecsocket.sokol.paper.util.asCompositePath
import com.gitlab.aecsocket.sokol.paper.util.makeCompositePath
import com.jme3.bullet.collision.shapes.CollisionShape
import com.jme3.bullet.collision.shapes.CompoundCollisionShape
import com.jme3.bullet.objects.PhysicsGhostObject
import com.jme3.bullet.objects.PhysicsRigidBody
import com.jme3.bullet.objects.PhysicsVehicle
import com.jme3.math.Quaternion
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required
import java.util.UUID

private const val MASS = "mass"
private const val BODY = "body"
private const val ID = "id"
private const val CENTER_OF_MASS = "center_of_mass"
private const val COMPOSITE_MAP = "composite_map"

data class Collider(
    val profile: Profile,
    var mass: Float?,
    var bodyData: BodyData?,
) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("collider")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    @ConfigSerializable
    data class BodyData(
        val bodyId: UUID,
        var centerOfMass: Vector3,
        var compositeMap: List<CompositePath>,
    )

    data class BodyInstance(
        val body: SokolPhysicsObject,
        val physicsSpace: ServerPhysicsSpace
    )

    override val componentType get() = Collider::class
    override val key get() = Key

    lateinit var backingPosition: PositionWrite
    var body: BodyInstance? = null

    fun mass() = mass ?: profile.mass

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()
        .setOrClear(MASS) { mass?.let { body -> makeFloat(body) } }
        .setOrClear(BODY) { bodyData?.let { body -> makeCompound()
            .set(ID) { makeUUID(body.bodyId) }
            .set(CENTER_OF_MASS) { makeVector3(body.centerOfMass) }
            .set(COMPOSITE_MAP) { makeList().apply { body.compositeMap.forEach { path -> add { makeCompositePath(path) } } } }
        } }

    override fun write(node: ConfigurationNode) {
        node.node(MASS).set(mass)
        node.node(BODY).set(bodyData)
    }

    @ConfigSerializable
    data class Profile(
        @Required val shape: Shape,
        val mass: Float = 1f
    ) : ComponentProfile {
        override fun read(tag: NBTTag) = tag.asCompound().run { Collider(this@Profile,
            getOr(MASS) { asFloat() },
            getOr(BODY) { asCompound().run { BodyData(
                get(ID) { asUUID() },
                get(CENTER_OF_MASS) { asVector3() },
                get(COMPOSITE_MAP) { asList().map { it.asCompositePath() } }
            ) } }
        ) }

        override fun read(node: ConfigurationNode) = Collider(this,
            node.node(MASS).getIfExists(),
            node.node(BODY).getIfExists()
        )

        override fun readEmpty() = Collider(this, null, null)
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

object GhostBody : PersistentComponent {
    override val componentType get() = GhostBody::class
    override val key = SokolAPI.key("ghost_body")
    val Type = ComponentType.singletonComponent(key, GhostBody)

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()

    override fun write(node: ConfigurationNode) {}
}

interface SokolPhysicsObject : TrackedPhysicsObject {
    var entity: SokolEntity
}

@All(Collider::class, CompositeTransform::class, CompositePathed::class)
@After(CompositeTransformSystem::class, CompositePathedSystem::class)
class ColliderBuildSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mCollider = mappers.componentMapper<Collider>()
    private val mCompositeTransform = mappers.componentMapper<CompositeTransform>()
    private val mCompositePath = mappers.componentMapper<CompositePathed>()
    private val mComposite = mappers.componentMapper<Composite>()

    @Subscribe
    fun on(event: BuildBody, entity: SokolEntity) {
        val collider = mCollider.get(entity)
        val compositeTransform = mCompositeTransform.get(entity)
        val compositePath = mCompositePath.get(entity)

        event.addBody(
            collisionOf(collider.profile.shape),
            compositeTransform.transform,
            collider.mass(),
            compositePath.path,
        )

        mComposite.forward(entity, event)
    }

    data class BodyData(
        val path: CompositePath,
        val shape: CollisionShape,
        val transform: Transform,
        val mass: Float,
    )

    data class BuildBody(
        val bodies: MutableList<BodyData> = ArrayList()
    ) : SokolEvent {
        fun addBody(shape: CollisionShape, transform: Transform, mass: Float, path: CompositePath) {
            if (mass < 0)
                throw IllegalArgumentException("Cannot have negative mass")
            bodies.add(BodyData(path, shape, transform, mass))
        }
    }
}

@All(Collider::class, PositionWrite::class, Removable::class)
@One(RigidBody::class, VehicleBody::class, GhostBody::class)
class ColliderSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mPosition = mappers.componentMapper<PositionWrite>()
    private val mCollider = mappers.componentMapper<Collider>()
    private val mRemovable = mappers.componentMapper<Removable>()
    private val mRigidBody = mappers.componentMapper<RigidBody>()
    private val mVehicleBody = mappers.componentMapper<VehicleBody>()
    private val mGhostBody = mappers.componentMapper<GhostBody>()

    private data class FullBodyData(
        val shape: CollisionShape,
        val mass: Float,
        val centerOfMass: Vector3,
        val compositeMap: List<CompositePath>,
    )

    private fun buildBody(entity: SokolEntity): FullBodyData {
        val (parts) = entity.call(ColliderBuildSystem.BuildBody())

        val totalMass = parts.map { it.mass }.sum()
        var centerOfMass = Vector3.Zero
        parts.forEach { (_, _, transform, mass) ->
            val portionOfTotal = mass / totalMass
            centerOfMass += transform.translation * portionOfTotal.toDouble()
        }

        val compoundShape = CompoundCollisionShape()
        val compositeMap = ArrayList<CompositePath>()
        parts.forEach { (path, shape, transform) ->
            val newTransform = Transform(
                transform.translation - centerOfMass,
                transform.rotation
            )

            val added = compoundShape.addShape(shape, newTransform.bullet())
            repeat(added) {
                compositeMap.add(path)
            }
        }

        return FullBodyData(compoundShape, totalMass, centerOfMass, compositeMap)
    }

    @Subscribe
    fun on(event: Rebuild, entity: SokolEntity) {
        val position = mPosition.get(entity)
        val collider = mCollider.get(entity)

        val bodyData = collider.bodyData ?: return
        val physSpace = CraftBulletAPI.spaceOf(position.world)
        val tracked = physSpace.trackedBy(bodyData.bodyId) ?: return
        val body = tracked.body

        val oldCom = bodyData.centerOfMass
        val (shape, mass, centerOfMass, compositeMap) = buildBody(entity)
        val deltaCom = centerOfMass - oldCom

        CraftBulletAPI.executePhysics {
            body.collisionShape = shape
            // when rebuilding the body, we are going to move the center-of-mass
            // to avoid the physical position in world also being altered, we offset it back
            // since we write to the position based on the physPosition
            body.transform = com.jme3.math.Transform(deltaCom.bullet(), Quaternion.IDENTITY).combineWithParent(body.transform)
            if (body is PhysicsRigidBody) body.mass = mass
        }

        bodyData.centerOfMass = centerOfMass
        bodyData.compositeMap = compositeMap
    }

    @Subscribe
    fun on(event: SokolEvent.Populate, entity: SokolEntity) {
        val collider = mCollider.get(entity)
        val position = mPosition.get(entity)
        collider.backingPosition = position

        val bodyData = collider.bodyData ?: return
        val physSpace = CraftBulletAPI.spaceOf(position.world)
        val obj = physSpace.trackedBy(bodyData.bodyId) as? SokolPhysicsObject ?: return

        collider.body = Collider.BodyInstance(obj, physSpace)

        mPosition.set(entity, object : PositionWrite {
            override val world get() = position.world
            override var transform: Transform
                get() = position.transform
                set(value) {
                    position.transform = value
                    CraftBulletAPI.executePhysics {
                        obj.body.transform = (value + Transform(bodyData.centerOfMass)).bullet()
                    }
                }
        })
    }

    @Subscribe
    fun on(event: SokolEvent.Add, entity: SokolEntity) {
        val collider = mCollider.get(entity)
        val position = collider.backingPosition
        val removable = mRemovable.get(entity)

        val (shape, mass, centerOfMass, compositeMap) = buildBody(entity)

        val id = UUID.randomUUID()
        val physSpace = CraftBulletAPI.spaceOf(position.world)

        val transform = position.transform
        CraftBulletAPI.executePhysics {
            val typeField =
                (if (mRigidBody.has(entity)) 0x1 else 0) or
                (if (mVehicleBody.has(entity)) 0x2 else 0) or
                (if (mGhostBody.has(entity)) 0x4 else 0)

            val physPosition = transform.translation.bullet()
            val physRotation = transform.rotation.bullet()
            val body = when (typeField) {
                0x1 -> object : PhysicsRigidBody(shape, mass), SokolPhysicsObject {
                    override val id get() = id
                    override val body get() = this
                    override var entity = entity

                    override fun update(ctx: TrackedPhysicsObject.Context) {
                        if (removable.removed) ctx.remove()
                        entity.call(PhysicsUpdate(this))
                    }
                }.also {
                    it.physPosition = physPosition
                    it.physRotation = physRotation
                }
                0x2 -> object : PhysicsVehicle(shape, mass), SokolPhysicsObject {
                    override val id get() = id
                    override val body get() = this
                    override var entity = entity

                    override fun update(ctx: TrackedPhysicsObject.Context) {
                        if (removable.removed) ctx.remove()
                        entity.call(PhysicsUpdate(this))
                    }
                }.also {
                    it.physPosition = physPosition
                    it.physRotation = physRotation
                }
                0x4 -> object : PhysicsGhostObject(shape), SokolPhysicsObject {
                    override val id get() = id
                    override val body get() = this
                    override var entity = entity

                    override fun update(ctx: TrackedPhysicsObject.Context) {
                        if (removable.removed) ctx.remove()
                        entity.call(PhysicsUpdate(this))
                    }
                }.also {
                    it.physPosition = physPosition
                    it.physRotation = physRotation
                }
                else -> throw IllegalStateException("Multiple body types defined for collider")
            }

            physSpace.addCollisionObject(body)
        }

        collider.bodyData = Collider.BodyData(id, centerOfMass, compositeMap)
    }

    private fun remove(entity: SokolEntity) {
        val collider = mCollider.get(entity)
        val (body, physSpace) = collider.body ?: return

        collider.bodyData = null
        CraftBulletAPI.executePhysics {
            // I hate this, but for some reason the object can already be removed, and it will throw warning
            // So Remove event can be called multiple times??? I don't know
            physSpace.removeTracked(body.id)
        }
    }

    @Subscribe
    fun on(event: SokolEvent.Reset, entity: SokolEntity) {
        remove(entity)
    }

    @Subscribe
    fun on(event: SokolEvent.Remove, entity: SokolEntity) {
        remove(entity)
    }

    @Subscribe
    fun on(event: SokolEvent.Update, entity: SokolEntity) {
        val collider = mCollider.get(entity)
        val position = collider.backingPosition
        val bodyData = collider.bodyData ?: return
        val (tracked) = collider.body ?: return
        val body = tracked.body

        tracked.entity = entity
        position.transform = Transform(
            body.physPosition.alexandria(),
            body.physRotation.alexandria(),
        ) + Transform(-bodyData.centerOfMass)
    }

    @Subscribe
    fun on(event: SokolEvent.Reload, entity: SokolEntity) {
        entity.call(Rebuild)
    }

    @Subscribe
    fun on(event: Composite.TreeMutate, entity: SokolEntity) {
        entity.call(Rebuild)
    }

    object Rebuild : SokolEvent

    // NB: modifying component data during this will not persist
    // use SupplierEntityAccess instead
    data class PhysicsUpdate(
        val body: TrackedPhysicsObject
    ) : SokolEvent
}
