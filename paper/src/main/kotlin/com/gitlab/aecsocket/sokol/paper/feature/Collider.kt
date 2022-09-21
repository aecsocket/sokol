package com.gitlab.aecsocket.sokol.paper.feature

import com.gitlab.aecsocket.alexandria.core.extension.getIfExists
import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.craftbullet.core.TrackedPhysicsObject
import com.gitlab.aecsocket.craftbullet.core.physPosition
import com.gitlab.aecsocket.craftbullet.core.physRotation
import com.gitlab.aecsocket.craftbullet.paper.CraftBulletAPI
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.*
import com.jme3.bullet.collision.shapes.BoxCollisionShape
import com.jme3.bullet.objects.PhysicsRigidBody
import org.spongepowered.configurate.ConfigurationNode
import java.util.UUID

private const val BODY_ID = "body_id"

data class Collider(
    var bodyId: UUID? = null
) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("collider")
    }

    override val componentType get() = Collider::class.java
    override val key get() = Key

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()
        .setOrClear(BODY_ID) { bodyId?.let { makeUUID(it) } }

    override fun write(node: ConfigurationNode) {
        node.node(BODY_ID).set(bodyId)
    }

    object Type : PersistentComponentType {
        override val key get() = Key

        override fun read(tag: NBTTag) = tag.asCompound().run { Collider(
            getOr(BODY_ID) { asUUID() }
        ) }

        override fun read(node: ConfigurationNode) = Collider(
            node.node(BODY_ID).getIfExists()
        )
    }
}

@All(Location::class, Collider::class, IsValidSupplier::class)
class ColliderSystem(engine: SokolEngine) : SokolSystem {
    private val mLocation = engine.componentMapper<Location>()
    private val mCollider = engine.componentMapper<Collider>()
    private val mIsValidSupplier = engine.componentMapper<IsValidSupplier>()

    @Subscribe
    fun on(event: SokolEvent.Add, entity: SokolEntityAccess) {
        val location = mLocation.map(entity)
        val collider = mCollider.map(entity)
        val isValid = mIsValidSupplier.map(entity).valid

        val shape = BoxCollisionShape(0.5f)
        val mass = 5f

        val id = UUID.randomUUID()
        val physSpace = CraftBulletAPI.spaceOf(location.world)

        val transform = location.transform
        CraftBulletAPI.executePhysics {
            physSpace.addCollisionObject(object : PhysicsRigidBody(shape, mass), TrackedPhysicsObject {
                override val id get() = id
                override val body get() = this

                override fun update(ctx: TrackedPhysicsObject.Context) {
                    if (!isValid())
                        ctx.remove()
                }
            }.also {
                it.physPosition = transform.translation.bullet()
                it.physRotation = transform.rotation.bullet()
            })
        }

        // TODO persist this somehow
        collider.bodyId = id
    }

    @Subscribe
    fun on(event: SokolEvent.Update, entity: SokolEntityAccess) {
        val location = mLocation.map(entity)
        val collider = mCollider.map(entity)

        collider.bodyId?.let { bodyId ->
            val physSpace = CraftBulletAPI.spaceOf(location.world)
            physSpace.trackedBy(bodyId)?.let { physBody ->
                // TODO persist / teleport entity
                // TODO run this in phys thread? idk
                location.transform = Transform(
                    physBody.body.physPosition.alexandria(),
                    physBody.body.physRotation.alexandria(),
                )
            } ?: run {
                // TODO persist
                collider.bodyId = null
            }
        }
    }

    @Subscribe
    fun on(event: SokolEvent.Remove, entity: SokolEntityAccess) {
        val location = mLocation.map(entity)
        val collider = mCollider.map(entity)

        collider.bodyId?.let { bodyId ->
            val physSpace = CraftBulletAPI.spaceOf(location.world)
            CraftBulletAPI.executePhysics {
                physSpace.removeTracked(bodyId)
            }

            // TODO persist
            collider.bodyId = null
        }
    }
}
