package com.gitlab.aecsocket.sokol.paper.feature

import com.gitlab.aecsocket.craftbullet.paper.CraftBulletAPI
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.ByEntityEvent
import com.gitlab.aecsocket.sokol.paper.HostedByEntity
import java.util.UUID

private const val COLLIDER = "collider"
private const val BODY_ID = "body_id"

data class Collider(
    val bodyId: UUID
) : SokolComponent {
    override val type get() = Collider

    companion object : ComponentType<Collider>
}

class ColliderSystem(engine: SokolEngine) : SokolSystem {
    private val bullet = CraftBulletAPI

    private val entFilter = engine.entityFilter(
        setOf(HostedByEntity)
    )
    private val mMob = engine.componentMapper(HostedByEntity)
    private val mCollider = engine.componentMapper(Collider)

    override fun handle(space: SokolEngine.Space, event: SokolEvent) = when (event) {
        is ByEntityEvent.Added -> {
            space.entitiesBy(entFilter).forEach { entityId ->
                val mob = mMob.map(space, entityId)
                val collider = mCollider.map(space, entityId)
            }
        }
        else -> {}
    }
    /*
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
    }*/
}
