package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.paper.*
import com.gitlab.aecsocket.alexandria.paper.extension.bukkitPlayers
import com.gitlab.aecsocket.craftbullet.paper.CraftBulletAPI
import com.gitlab.aecsocket.craftbullet.paper.rayTestFrom
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.component.Held
import com.gitlab.aecsocket.sokol.paper.component.SokolPhysicsObject
import com.jme3.bullet.collision.PhysicsRayTestResult
import org.bukkit.entity.Player

interface HoldOperation {
    val canRelease: Boolean
}

class EntityHolding internal constructor(
    private val sokol: Sokol
) : PlayerFeature<EntityHolding.PlayerData> {
    data class ChangeHoverState(
        val player: Player,
        val hovered: Boolean,
        val rayTest: PhysicsRayTestResult
    ) : SokolEvent

    data class ChangeHoldState(
        val held: Boolean
    ) : SokolEvent

    data class Hover(
        val physObj: SokolPhysicsObject,
        val rayTest: PhysicsRayTestResult
    )

    data class Hold(
        val player: Player,
        var entity: SokolEntity,
        val operation: HoldOperation,
        val raiseHandLock: PlayerLockInstance
    ) {
        var frozen = false
        var drawSlotShapes = false
    }

    inner class PlayerData(val player: AlexandriaPlayer) : PlayerFeature.PlayerData {
        var hover: Hover? = null
        var hold: Hold? = null

        override fun dispose() {
            hold?.let { stop(player, it, false) }
        }
    }

    override fun createFor(player: AlexandriaPlayer) = PlayerData(player)

    private lateinit var mHeld: ComponentMapper<Held>

    internal fun enable() {
        mHeld = sokol.engine.mapper()

        sokol.onInput { event ->
            val holding = event.player.alexandria.featureData(this)
            (holding.hold?.entity ?: holding.hover?.physObj?.entity)?.callSingle(event)
        }

        CraftBulletAPI.onPostStep {
            bukkitPlayers.forEach { player ->
                val holding = player.alexandria.featureData(this)
                if (holding.hold != null) return@forEach
                val hover = holding.hover

                val newHover = player
                    .rayTestFrom(sokol.settings.entityHoverDistance)
                    .firstOrNull()?.let {
                        val obj = it.collisionObject
                        if (obj is SokolPhysicsObject) Hover(obj, it) else null
                    }

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

    private fun stop(player: AlexandriaPlayer, state: Hold, releaseLock: Boolean) {
        state.entity.callSingle(ChangeHoldState(false))
        mHeld.remove(state.entity)
        if (releaseLock) {
            player.releaseLock(state.raiseHandLock)
        }
    }

    fun stop(player: AlexandriaPlayer) {
        val holding = player.featureData(this)
        holding.hold?.let {
            stop(player, it, true)
            holding.hold = null
        }
    }

    fun stop(hold: Hold) {
        stop(hold.player.alexandria, hold, true)
        hold.player.alexandria.featureData(this).hold = null
    }

    fun start(player: AlexandriaPlayer, entity: SokolEntity, operation: HoldOperation) {
        stop(player)
        val hold = Hold(
            player.handle,
            entity,
            operation,
            player.acquireLock(PlayerLock.RaiseHand)
        )
        mHeld.set(entity, Held(hold))
        entity.callSingle(ChangeHoldState(true))
        player.featureData(this).hold = hold
    }
}

val AlexandriaPlayer.holding get() = featureData(SokolAPI.holding)
