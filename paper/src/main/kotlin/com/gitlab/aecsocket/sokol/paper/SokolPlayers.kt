package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.paper.AlexandriaAPI
import com.gitlab.aecsocket.alexandria.paper.AlexandriaPlayer
import com.gitlab.aecsocket.alexandria.paper.PlayerFeature
import com.gitlab.aecsocket.alexandria.paper.alexandria
import com.gitlab.aecsocket.alexandria.paper.extension.bukkitPlayers
import com.gitlab.aecsocket.alexandria.paper.extension.position
import com.gitlab.aecsocket.craftbullet.core.physPosition
import com.gitlab.aecsocket.craftbullet.paper.CraftBulletAPI
import com.gitlab.aecsocket.sokol.core.ComponentMapper
import com.gitlab.aecsocket.sokol.core.SokolEntity
import com.gitlab.aecsocket.sokol.core.extension.alexandria
import com.gitlab.aecsocket.sokol.core.extension.bullet
import com.gitlab.aecsocket.sokol.core.mapper
import com.gitlab.aecsocket.sokol.paper.component.*
import com.jme3.bullet.collision.shapes.BoxCollisionShape
import com.jme3.bullet.objects.PhysicsGhostObject

class SokolPlayers internal constructor(
    private val sokol: Sokol
) : PlayerFeature<SokolPlayers.PlayerData> {
    class PlayerData : PlayerFeature.PlayerData {
        var drawHoverShape = false
        var drawSlots = false
    }

    override fun createFor(player: AlexandriaPlayer) = PlayerData()

    private lateinit var mIsRoot: ComponentMapper<IsRoot>
    private lateinit var mPositionAccess: ComponentMapper<PositionAccess>
    private lateinit var mHoverShape: ComponentMapper<HoverShape>
    private lateinit var mEntitySlot: ComponentMapper<EntitySlot>

    internal fun enable() {
        mIsRoot = sokol.engine.mapper()
        mPositionAccess = sokol.engine.mapper()
        mHoverShape = sokol.engine.mapper()
        mEntitySlot = sokol.engine.mapper()
    }

    internal fun postPhysicsStep() {
        bukkitPlayers.forEach { player ->
            val sokolPlayer = player.alexandria.featureData(this)

            val physSpace = CraftBulletAPI.spaceOf(player.world)
            val ghostObject = PhysicsGhostObject(BoxCollisionShape(sokol.settings.drawRadius.toFloat()))
            ghostObject.physPosition = player.location.position().bullet()
            physSpace.addCollisionObject(ghostObject)

            val nearby: List<SokolEntity> = ghostObject.overlappingObjects.flatMap { body ->
                (body as? SokolPhysicsObject)?.entity?.let { entity ->
                    if (!mIsRoot.has(entity)) return@let emptyList()
                    entity.allEntitiesRecursive()
                } ?: emptyList()
            }

            physSpace.removeCollisionObject(ghostObject)

            val effector = player.alexandria.effector
            nearby.forEach nearby@ { entity ->
                val transform = mPositionAccess.getOr(entity)?.transform ?: return@nearby

                if (sokolPlayer.drawHoverShape) {
                    mHoverShape.getOr(entity)?.let { hoverShape ->
                        CraftBulletAPI
                            .drawPointsShape(hoverShape.profile.shape.bullet())
                            .forEach {
                                AlexandriaAPI.particles.spawn(sokol.settings.drawHoverShape, transform.apply(it.alexandria()), effector)
                            }
                    }
                }

                if (sokolPlayer.drawSlots) {
                    mEntitySlot.getOr(entity)?.let { entitySlot ->
                        CraftBulletAPI
                            .drawPointsShape(entitySlot.shape.bullet())
                            .forEach {
                                AlexandriaAPI.particles.spawn(sokol.settings.drawSlots, transform.apply(it.alexandria()), effector)
                            }
                    }
                }
            }
        }
    }
}

val AlexandriaPlayer.sokol get() = featureData(SokolAPI.players)
