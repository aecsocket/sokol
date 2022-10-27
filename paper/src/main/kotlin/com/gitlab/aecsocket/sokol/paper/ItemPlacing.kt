package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.core.extension.*
import com.gitlab.aecsocket.alexandria.core.physics.*
import com.gitlab.aecsocket.alexandria.paper.*
import com.gitlab.aecsocket.alexandria.paper.extension.*
import com.gitlab.aecsocket.craftbullet.core.hitNormal
import com.gitlab.aecsocket.craftbullet.paper.CraftBulletAPI
import com.gitlab.aecsocket.craftbullet.paper.bullet
import com.gitlab.aecsocket.sokol.core.extension.alexandria
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import kotlin.math.PI
import kotlin.math.abs

class ItemPlacing(
    private val sokol: Sokol
) : PlayerFeature<ItemPlacing.PlayerData> {
    data class Part(
        val mesh: Mesh,
        val transform: Transform,
    )

    data class State(
        val slotId: Int,
        var raiseHandLock: PlayerLockInstance,
        val parts: List<Part>,
        val placeTransform: Transform,
        val holdDistance: Double,
        val snapDistance: Double,

        var nextTransform: Transform? = null,
    )

    inner class PlayerData(
        private val player: AlexandriaPlayer,
        var state: State? = null
    ) : PlayerFeature.PlayerData {
        override fun update() {
            state?.let { state ->
                val from = player.handle.eyeLocation
                val direction = from.direction

                val snapDistance = state.snapDistance
                val snapTo = from + direction * snapDistance
                val physSpace = CraftBulletAPI.spaceOf(from.world)

                CraftBulletAPI.executePhysics {
                    val playerBody = player.handle
                    val result = physSpace.rayTest(from.bullet(), snapTo.bullet())
                        .firstOrNull { it.collisionObject !== playerBody }

                    state.nextTransform = if (result == null) {
                        Transform(
                            (from + direction * state.holdDistance).position(),
                            from.rotation()
                        )
                    } else {
                        val hitPos = from.position() + direction * (snapDistance * result.hitFraction)

                        val dir = result.hitNormal.alexandria()
                        val rotation = if (abs(dir.dot(Vector3.Up)) > 0.99) {
                            // `dir` and `up` are (close to) collinear
                            val yaw = player.handle.location.yaw.radians.toDouble()
                            // `rotation` will be facing "away" from the player
                            quaternionFromTo(Vector3.Forward, dir) *
                                Euler3(z = (if (dir.y > 0.0) -yaw else yaw) + PI).quaternion(EulerOrder.XYZ)
                        } else {
                            val v1 = Vector3.Up.cross(dir).normalized
                            val v2 = dir.cross(v1).normalized
                            quaternionOfAxes(v1, v2, dir)
                        }

                        Transform(hitPos, rotation)
                    }
                }

                state.parts.forEach { (mesh, transform) ->
                    state.nextTransform?.let { mesh.transform = it + state.placeTransform + transform }
                }
            }
        }
    }

    override fun createFor(player: AlexandriaPlayer) = PlayerData(player)

    internal fun enable() {
        sokol.registerEvents(object : Listener {
            @EventHandler(priority = EventPriority.HIGH)
            fun on(event: PlayerInteractEvent) {
                val player = event.player.alexandria
                val data = player.featureData(this@ItemPlacing)
                data.state?.let { state ->
                    if (event.action.isLeftClick) {
                        exitInternal(player, state)
                        data.state = null
                        return
                    }

                    event.isCancelled = true
                    if (!event.action.isRightClick) return

                }
            }

            @EventHandler(priority = EventPriority.HIGH)
            fun on(event: InventoryClickEvent) {
                val player = (event.whoClicked as? Player ?: return).alexandria
                player.featureData(this@ItemPlacing).state?.let { state ->
                    val slotId = state.slotId
                    if (event.slot == slotId || event.hotbarButton == slotId) {
                        event.isCancelled = true
                    }
                }
            }
        })
    }

    private fun exitInternal(player: AlexandriaPlayer, state: State) {
        player.releaseLock(state.raiseHandLock)
        state.parts.forEach { (mesh) ->
            mesh.remove(player.handle)
        }
    }

    fun enter(player: AlexandriaPlayer, state: State) {
        val data = player.featureData(this)
        data.state?.let { exitInternal(player, it) }
        data.state = state

        state.parts.forEach { (mesh) ->
            mesh.spawn(player.handle)
            mesh.glowing(true, player.handle)
        }
    }

    fun exit(player: AlexandriaPlayer) {
        val data = player.featureData(this)
        data.state?.let { state ->
            exitInternal(player, state)
            data.state = null
        }
    }
}
