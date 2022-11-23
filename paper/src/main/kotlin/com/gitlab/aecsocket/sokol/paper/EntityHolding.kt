package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.paper.AlexandriaPlayer
import com.gitlab.aecsocket.alexandria.paper.PlayerFeature
import com.gitlab.aecsocket.alexandria.paper.alexandria
import com.gitlab.aecsocket.alexandria.paper.extension.bukkitPlayers
import com.gitlab.aecsocket.craftbullet.paper.CraftBulletAPI
import com.gitlab.aecsocket.craftbullet.paper.rayTestFrom
import com.gitlab.aecsocket.sokol.core.SokolEvent
import com.gitlab.aecsocket.sokol.core.call
import com.gitlab.aecsocket.sokol.core.callSingle
import com.gitlab.aecsocket.sokol.paper.component.SokolPhysicsObject
import com.jme3.bullet.collision.PhysicsRayTestResult
import org.bukkit.entity.Player

class EntityHolding internal constructor(
    private val sokol: Sokol
) : PlayerFeature<EntityHolding.PlayerData> {
    data class ChangeHoverState(
        val player: Player,
        val hovered: Boolean,
        val rayTest: PhysicsRayTestResult
    ) : SokolEvent

    data class Hover(
        val physObj: SokolPhysicsObject,
        val rayTest: PhysicsRayTestResult
    )

    inner class PlayerData(val player: AlexandriaPlayer) : PlayerFeature.PlayerData {
        var hover: Hover? = null
    }

    override fun createFor(player: AlexandriaPlayer) = PlayerData(player)

    internal fun enable() {
        sokol.onInput { event ->
            val holding = event.player.alexandria.featureData(this)
            holding.hover?.physObj?.entity?.call(event)
        }

        CraftBulletAPI.onPostStep {
            bukkitPlayers.forEach { player ->
                val holding = player.alexandria.featureData(this)
                val newHover = player
                    .rayTestFrom(sokol.settings.entityHoverDistance)
                    .firstOrNull()?.let {
                        val obj = it.collisionObject
                        if (obj is SokolPhysicsObject) Hover(obj, it) else null
                    }

                val hover = holding.hover
                hover?.let {
                    if (newHover == null || newHover.physObj !== hover.physObj) {
                        hover.physObj.entity.callSingle(ChangeHoverState(player, false, hover.rayTest))
                    }
                }
                newHover?.let {
                    if (hover == null || hover.physObj !== newHover.physObj) {
                        newHover.physObj.entity.callSingle(ChangeHoverState(player, true, newHover.rayTest))
                    }
                }
                holding.hover = newHover
            }
        }
    }
}
