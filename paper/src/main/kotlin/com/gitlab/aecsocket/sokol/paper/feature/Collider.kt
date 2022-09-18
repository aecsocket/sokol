package com.gitlab.aecsocket.sokol.paper.feature

import com.gitlab.aecsocket.alexandria.paper.extension.bukkitPlayers
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.alexandria.paper.extension.position
import com.gitlab.aecsocket.craftbullet.core.TrackedPhysicsObject
import com.gitlab.aecsocket.craftbullet.core.physPosition
import com.gitlab.aecsocket.craftbullet.paper.CraftBulletAPI
import com.gitlab.aecsocket.craftbullet.paper.location
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.ByEntityEvent
import com.gitlab.aecsocket.sokol.paper.HostedByEntity
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import com.jme3.bullet.collision.shapes.BoxCollisionShape
import com.jme3.bullet.objects.PhysicsRigidBody
import com.jme3.math.Vector3f
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import java.util.UUID

private const val COLLIDER = "collider"
private const val BODY_ID = "body_id"

class ColliderComponent : SokolComponentType {
    override val key get() = ColliderComponent.key

    override fun deserialize(node: ConfigurationNode) = Component(
        node.node(BODY_ID).get<UUID>()
    )

    override fun deserialize(tag: CompoundNBTTag) = Component(
        tag.uuidOr(BODY_ID)
    )

    inner class Component(
        var bodyId: UUID? = null
    ) : SokolComponent.Persistent {
        override val key get() = ColliderComponent.key

        override fun serialize(node: ConfigurationNode) {
            bodyId?.let { node.node(BODY_ID).set(it) }
        }

        override fun serialize(tag: CompoundNBTTag.Mutable) {
            bodyId?.let { tag.set(BODY_ID) { ofUUID(it) } }
        }
    }

    companion object : ComponentKey<Component> {
        override val key = SokolAPI.key(COLLIDER)
    }
}

private val filter = entityFilterOf(setOf(
    HostedByEntity.key,
    ColliderComponent.key
))

class ColliderSystem(
    private val sokol: Sokol
) : SokolSystem {
    private val bullet = CraftBulletAPI

    override fun handle(entities: EntityAccessor, event: SokolEvent) {
        when (event) {
            is ByEntityEvent.Added -> {
                entities.by(filter).forEach { entity ->
                    val mob = entity.force(HostedByEntity).entity
                    val collider = entity.force(ColliderComponent)

                    // TODO wtf?!?!?!
                    val shape = BoxCollisionShape(0.5f)
                    val mass = 60f

                    val id = UUID.randomUUID()
                    val space = bullet.spaceOf(mob.world)
                    bullet.executePhysics {
                        space.addCollisionObject(object : PhysicsRigidBody(shape, mass), TrackedPhysicsObject {
                            override val id get() = id
                            override val body get() = this

                            override fun update(ctx: TrackedPhysicsObject.Context) {
                                if (!mob.isValid) {
                                    ctx.remove()
                                }
                            }
                        }.also {
                            it.physPosition = mob.location.position().bullet()
                        })
                    }
                    collider.bodyId = id
                }
            }
            is ByEntityEvent.Removed -> {
                entities.by(filter).forEach { entity ->
                    val mob = entity.force(HostedByEntity).entity
                    val collider = entity.force(ColliderComponent)

                    collider.bodyId?.let { bodyId ->
                        val space = bullet.spaceOf(mob.world)
                        bullet.executePhysics {
                            space.removeTracked(bodyId)
                        }
                    }
                    collider.bodyId = null
                }
            }
            is UpdateEvent -> {
                entities.by(filter).forEach { entity ->
                    val mob = entity.force(HostedByEntity).entity
                    val collider = entity.force(ColliderComponent)

                    collider.bodyId?.let { bodyId ->
                        bullet.spaceOf(mob.world).trackedBy(bodyId)?.let { body ->
                            bullet.executePhysics {
                                mob.teleportAsync(body.body.physPosition.location(mob.world))
                            }
                        } ?: run {
                            collider.bodyId = null
                        }
                    }
                }
            }
        }
    }
}
