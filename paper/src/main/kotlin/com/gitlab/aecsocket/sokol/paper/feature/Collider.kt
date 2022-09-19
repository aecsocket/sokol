package com.gitlab.aecsocket.sokol.paper.feature

import com.gitlab.aecsocket.alexandria.core.physics.Quaternion
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.alexandria.paper.extension.position
import com.gitlab.aecsocket.craftbullet.core.TrackedPhysicsObject
import com.gitlab.aecsocket.craftbullet.core.physPosition
import com.gitlab.aecsocket.craftbullet.core.physRotation
import com.gitlab.aecsocket.craftbullet.paper.CraftBulletAPI
import com.gitlab.aecsocket.craftbullet.paper.location
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.extension.asQuaternion
import com.gitlab.aecsocket.sokol.core.extension.ofQuaternion
import com.gitlab.aecsocket.sokol.paper.*
import com.jme3.bullet.collision.shapes.BoxCollisionShape
import com.jme3.bullet.objects.PhysicsRigidBody
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required
import java.util.UUID

private const val ID = "id"
private const val ROTATION = "rotation"

data class Collider(
    var body: BodyData? = null,
) : PersistentComponent {
    @ConfigSerializable
    data class BodyData(
        @Required val id: UUID,
        @Required var rotation: Quaternion,
    )

    override val type get() = Collider
    override val key get() = Key

    override fun write(tag: CompoundNBTTag.Mutable) {
        body?.let { tag
            .set(ID) { ofUUID(it.id) }
            .set(ROTATION) { ofQuaternion(it.rotation) }
        } ?: tag.clear()
    }

    override fun write(node: ConfigurationNode) {
        body?.let { node.set(it) }
    }

    class Type : PersistentComponentType {
        override val key get() = Key

        override fun read(tag: CompoundNBTTag) = Collider(
            if (tag.contains(ID)) BodyData(
                tag.get(ID) { asUUID() },
                tag.get(ROTATION) { asQuaternion() },
            ) else null
        )

        override fun read(node: ConfigurationNode) = Collider(
            if (node.hasChild(ID)) node.get<BodyData>() else null
        )
    }

    companion object : ComponentType<Collider> {
        val Key = SokolAPI.key("collider")
    }
}

class ColliderSystem(engine: SokolEngine) : SokolSystem {
    private val bullet = CraftBulletAPI

    private val entFilter = engine.entityFilter(
        setOf(NBTTagAccessor, HostedByEntity, Collider)
    )
    private val mTagAccessor = engine.componentMapper(NBTTagAccessor)
    private val mMob = engine.componentMapper(HostedByEntity)
    private val mCollider = engine.componentMapper(Collider)

    override fun handle(space: SokolEngine.Space, event: SokolEvent) = when (event) {
        is ByEntityEvent.Added -> {
            space.entitiesBy(entFilter).forEach { entity ->
                val tagAccessor = mTagAccessor.map(space, entity)
                val mob = mMob.map(space, entity).entity
                val collider = mCollider.map(space, entity)

                // TODO wtf do we do here???
                val shape = BoxCollisionShape(0.5f)
                val mass = 5f

                val id = UUID.randomUUID()
                val physSpace = bullet.spaceOf(mob.world)
                bullet.executePhysics {
                    physSpace.addCollisionObject(object : PhysicsRigidBody(shape, mass), TrackedPhysicsObject {
                        override val id get() = id
                        override val body get() = this

                        override fun update(ctx: TrackedPhysicsObject.Context) {
                            if (!mob.isValid)
                                ctx.remove()
                        }
                    }.also {
                        it.physPosition = mob.location.position().bullet()
                    })
                }

                collider.body = Collider.BodyData(id, Quaternion.Identity)
                tagAccessor.write(collider)

                println("added collider $id")
            }
        }
        is ByEntityEvent.Removed -> {
            space.entitiesBy(entFilter).forEach { entity ->
                val tagAccessor = mTagAccessor.map(space, entity)
                val mob = mMob.map(space, entity).entity
                val collider = mCollider.map(space, entity)

                collider.body?.id?.let { bodyId ->
                    val physSpace = bullet.spaceOf(mob.world)
                    bullet.executePhysics {
                        physSpace.removeTracked(bodyId)
                    }

                    println("removed collider $bodyId")
                }

                collider.body = null
                tagAccessor.write(collider)
            }
        }
        is UpdateEvent -> {
            space.entitiesBy(entFilter).forEach { entity ->
                val tagAccessor = mTagAccessor.map(space, entity)
                val mob = mMob.map(space, entity).entity
                val collider = mCollider.map(space, entity)

                collider.body?.let { body ->
                    bullet.spaceOf(mob.world).trackedBy(body.id)?.let { physBody ->
                        body.rotation = physBody.body.physRotation.alexandria()
                        bullet.executePhysics {
                            mob.teleportAsync(physBody.body.physPosition.location(mob.world))
                        }
                    } ?: run {
                        collider.body = null
                    }
                }

                tagAccessor.write(collider)
            }
        }
        else -> {}
    }
}
