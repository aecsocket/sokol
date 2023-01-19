package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.paper.AlexandriaAPI
import com.gitlab.aecsocket.alexandria.paper.AlexandriaPlayer
import com.gitlab.aecsocket.alexandria.paper.PlayerFeature
import com.gitlab.aecsocket.alexandria.paper.alexandria
import com.gitlab.aecsocket.alexandria.paper.extension.bukkitPlayers
import com.gitlab.aecsocket.alexandria.paper.extension.location
import com.gitlab.aecsocket.alexandria.paper.extension.position
import com.gitlab.aecsocket.craftbullet.core.physPosition
import com.gitlab.aecsocket.craftbullet.paper.CraftBulletAPI
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.extension.alexandria
import com.gitlab.aecsocket.sokol.core.extension.bullet
import com.gitlab.aecsocket.sokol.paper.component.*

class SokolPlayers internal constructor(
    private val sokol: Sokol
) : PlayerFeature<SokolPlayers.PlayerData> {
    class PlayerData : PlayerFeature.PlayerData {
        var drawHoverShape = false
        var drawSlots = false
    }

    override fun createFor(player: AlexandriaPlayer) = PlayerData()

    private lateinit var mComposite: ComponentMapper<Composite>
    private lateinit var mIsChild: ComponentMapper<IsChild>
    private lateinit var mPositionAccess: ComponentMapper<PositionAccess>
    private lateinit var mHoverShape: ComponentMapper<HoverShape>
    private lateinit var mEntitySlot: ComponentMapper<EntitySlot>

    internal fun enable() {
        sokol.engine.apply {
            mComposite = mapper()
            mIsChild = mapper()
            mPositionAccess = mapper()
            mHoverShape = mapper()
            mEntitySlot = mapper()
        }
    }

    internal fun postPhysicsStep() {
        val debugDraw = sokol.settings.debugDraw
        bukkitPlayers.forEach { player ->
            val sokolPlayer = player.alexandria.featureData(this)
            val nearby = sokol.resolver.entitiesNear(
                CraftBulletAPI.spaceOf(player.world),
                player.location.position(),
                debugDraw.radius
            )
            val effector = player.alexandria.effector
            nearby.forEach nearby@ { entity ->
                val transform = mPositionAccess.getOr(entity)?.transform ?: return@nearby

                if (sokolPlayer.drawHoverShape) {
                    mHoverShape.getOr(entity)?.let { hoverShape ->
                        CraftBulletAPI
                            .drawPointsShape(hoverShape.profile.shape.bullet())
                            .forEach {
                                AlexandriaAPI.particles.spawn(debugDraw.hoverShape, transform.apply(it.alexandria()), effector)
                            }
                    }
                }

                if (sokolPlayer.drawSlots) {
                    mEntitySlot.getOr(entity)?.let { entitySlot ->
                        CraftBulletAPI
                            .drawPointsShape(entitySlot.shape.bullet())
                            .forEach {
                                AlexandriaAPI.particles.spawn(debugDraw.slots, transform.apply(it.alexandria()), effector)
                            }
                    }
                }
            }
        }
    }
}

val AlexandriaPlayer.sokol get() = featureData(SokolAPI.players)
