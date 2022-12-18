package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.core.physics.CollisionInfo
import com.gitlab.aecsocket.alexandria.core.physics.Ray
import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.core.physics.invert
import com.gitlab.aecsocket.alexandria.paper.*
import com.gitlab.aecsocket.alexandria.paper.extension.bukkitPlayers
import com.gitlab.aecsocket.alexandria.paper.extension.direction
import com.gitlab.aecsocket.alexandria.paper.extension.position
import com.gitlab.aecsocket.craftbullet.core.physPosition
import com.gitlab.aecsocket.craftbullet.paper.CraftBulletAPI
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.extension.bullet
import com.gitlab.aecsocket.sokol.paper.component.Held
import com.gitlab.aecsocket.sokol.paper.component.HoverShape
import com.gitlab.aecsocket.sokol.paper.component.PositionRead
import com.gitlab.aecsocket.sokol.paper.component.SokolPhysicsObject
import com.jme3.bullet.collision.shapes.BoxCollisionShape
import com.jme3.bullet.objects.PhysicsGhostObject
import org.bukkit.entity.Player

interface HoldOperation {
    val canRelease: Boolean
}

class EntityHolding internal constructor(
    private val sokol: Sokol
) : PlayerFeature<EntityHolding.PlayerData> {
    data class ChangeHoverState(
        val player: Player,
        val hovered: Boolean
    ) : SokolEvent

    data class ChangeHoldState(
        val held: Boolean
    ) : SokolEvent

    data class Hover(
        val physObj: SokolPhysicsObject,
        val collision: CollisionInfo
    )

    data class Hold(
        val player: Player,
        var entity: SokolEntity,
        val operation: HoldOperation,
        var nextTransform: Transform,
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
    private lateinit var mPositionRead: ComponentMapper<PositionRead>
    private lateinit var mHoverShape: ComponentMapper<HoverShape>

    internal fun enable() {
        mHeld = sokol.engine.mapper()
        mPositionRead = sokol.engine.mapper()
        mHoverShape = sokol.engine.mapper()

        sokol.onInput { event ->
            val holding = event.player.alexandria.featureData(this)
            (holding.hold?.entity ?: holding.hover?.physObj?.entity)?.callSingle(event)
        }

        CraftBulletAPI.onPostStep {
            bukkitPlayers.forEach { player ->
                val holding = player.alexandria.featureData(this)
                if (holding.hold != null) return@forEach
                val hover = holding.hover

                val physSpace = CraftBulletAPI.spaceOf(player.world)
                val location = player.eyeLocation
                val testGhost = PhysicsGhostObject(BoxCollisionShape(sokol.settings.entityHoverDistance.toFloat())).also {
                    it.physPosition = location.position().bullet()
                }

                physSpace.addCollisionObject(testGhost)
                val testBodies = testGhost.overlappingObjects
                physSpace.removeCollisionObject(testGhost)

                val ray = Ray(location.position(), location.direction())
                val newHover = testBodies.mapNotNull {
                    if (it !is SokolPhysicsObject) return@forEach
                    val entity = it.entity
                    val positionRead = mPositionRead.getOr(entity) ?: return@forEach
                    val hoverShape = mHoverShape.getOr(entity)?.profile ?: return@forEach

                    val transform = positionRead.transform
                    val collision = hoverShape.shape.testRay(transform.invert(ray)) ?: return@forEach
                    collision.tIn to Hover(it, collision)
                }.minByOrNull { (tIn) -> tIn }?.second

                hover?.let {
                    if (newHover == null || newHover.physObj !== hover.physObj) {
                        hover.physObj.entity.callSingle(ChangeHoverState(player, false))
                    }
                }
                newHover?.let {
                    if (hover == null || hover.physObj !== newHover.physObj) {
                        newHover.physObj.entity.callSingle(ChangeHoverState(player, true))
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

    fun start(player: AlexandriaPlayer, entity: SokolEntity, operation: HoldOperation, transform: Transform) {
        stop(player)
        val hold = Hold(
            player.handle,
            entity,
            operation,
            transform,
            player.acquireLock(PlayerLock.RaiseHand)
        )
        mHeld.set(entity, Held(hold))
        entity.callSingle(ChangeHoldState(true))
        player.featureData(this).hold = hold
    }
}

val AlexandriaPlayer.holding get() = featureData(SokolAPI.holding)
