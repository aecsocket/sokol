package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.core.extension.*
import com.gitlab.aecsocket.alexandria.core.physics.*
import com.gitlab.aecsocket.alexandria.paper.AlexandriaPlayer
import com.gitlab.aecsocket.alexandria.paper.Mesh
import com.gitlab.aecsocket.alexandria.paper.PlayerFeature
import com.gitlab.aecsocket.alexandria.paper.alexandria
import com.gitlab.aecsocket.alexandria.paper.extension.*
import com.gitlab.aecsocket.craftbullet.core.hitNormal
import com.gitlab.aecsocket.craftbullet.paper.CraftBulletAPI
import com.gitlab.aecsocket.craftbullet.paper.bullet
import com.gitlab.aecsocket.craftbullet.paper.executePhysics
import com.gitlab.aecsocket.sokol.core.extension.alexandria
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import kotlin.math.PI

class ItemPlacing(
    private val sokol: Sokol
) : PlayerFeature<ItemPlacing.PlayerData> {
    data class State(
        val slotId: Int,
        val meshes: List<Mesh>,
        val placeTransform: Transform,

        var snapTransform: Transform? = null,
    )

    inner class PlayerData(
        private val player: AlexandriaPlayer,
        var state: State? = null
    ) : PlayerFeature.PlayerData {
        override fun update() {
            state?.let { state ->
                val from = player.handle.eyeLocation
                val direction = from.direction
                val distance = 2.0
                val to = from + direction * distance
                val physSpace = CraftBulletAPI.spaceOf(from.world)

                player.handle.executePhysics { playerBody ->
                    val result = physSpace.rayTest(from.bullet(), to.bullet())
                        .firstOrNull { it.collisionObject !== playerBody }

                    state.snapTransform = if (result == null) null else {
                        val hitPos = from.position() + direction * (distance * result.hitFraction)
                        val dir = result.hitNormal.alexandria()
                        val v1 = Vector3.Up.cross(dir).normalized

                        // todo this code sucks and doesn't work
                        val rotation = if (v1.sqrLength < EPSILON) {
                            // `dir` and `up` are collinear
                            val yaw = player.handle.location.yaw.radians.toDouble()
                            // `rotation` will be facing "away" from the player
                            quaternionFromTo(Vector3.Forward, dir) *
                                Euler3(z = (if (dir.y > 0.0) -yaw else yaw) + PI).quaternion(EulerOrder.XYZ)
                        } else {
                            val v2 = dir.cross(v1).normalized
                            quaternionOfAxes(v1, v2, dir)
                        }

                        Transform(
                            hitPos,
                            rotation,
                        )
                    }
                }

                state.meshes.forEach { mesh ->
                    mesh.transform = (state.snapTransform ?: Transform(
                        (from + from.direction * 2.0).position(),
                        from.rotation()
                    )) + state.placeTransform
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
                data.state?.let {
                    if (event.action.isLeftClick) {
                        exit(player)
                        return
                    }

                    event.isCancelled = true
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
        state.meshes.forEach { mesh ->
            mesh.remove(player.handle)
        }
    }

    fun enter(player: AlexandriaPlayer, state: State) {
        val data = player.featureData(this)
        data.state?.let { exitInternal(player, it) }
        data.state = state

        state.meshes.forEach { mesh ->
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
