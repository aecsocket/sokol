package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.core.extension.*
import com.gitlab.aecsocket.alexandria.core.physics.*
import com.gitlab.aecsocket.alexandria.paper.*
import com.gitlab.aecsocket.alexandria.paper.extension.*
import com.gitlab.aecsocket.craftbullet.core.hitNormal
import com.gitlab.aecsocket.craftbullet.paper.CraftBullet
import com.gitlab.aecsocket.craftbullet.paper.CraftBulletAPI
import com.gitlab.aecsocket.craftbullet.paper.bullet
import com.gitlab.aecsocket.sokol.core.SokolEntity
import com.gitlab.aecsocket.sokol.core.extension.alexandria
import com.gitlab.aecsocket.sokol.core.extension.bullet
import com.gitlab.aecsocket.sokol.core.toBlueprint
import com.jme3.bullet.collision.shapes.CollisionShape
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import kotlin.math.PI
import kotlin.math.abs

class EntityHolding(
    private val sokol: Sokol
) : PlayerFeature<EntityHolding.PlayerData> {
    @ConfigSerializable
    data class StateSettings(
        val placeTransform: Transform,
        val holdDistance: Double,
        val snapDistance: Double,
    )

    data class Part(
        val mesh: Mesh,
        val transform: Transform,
    )

    data class State(
        val settings: StateSettings,
        val entity: SokolEntity,
        val slotId: Int,
        var raiseHandLock: PlayerLockInstance,
        var transform: Transform,
        val parts: List<Part>,

        var frozen: Boolean = false,
        var drawShape: CollisionShape? = null,
    )

    inner class PlayerData(
        private val player: AlexandriaPlayer,
        var state: State? = null
    ) : PlayerFeature.PlayerData {
        override fun update() {
            state?.let { state ->
                val from = player.handle.eyeLocation
                val direction = from.direction.alexandria()

                val snapDistance = state.settings.snapDistance
                val snapTo = from + direction * snapDistance
                val physSpace = CraftBulletAPI.spaceOf(from.world)

                CraftBulletAPI.executePhysics {
                    if (!state.frozen) {
                        val playerBody = player.handle
                        val result = physSpace.rayTestWorld(from.bullet(), snapTo.bullet())
                            .firstOrNull { it.collisionObject !== playerBody }

                        state.transform = if (result == null) {
                            Transform(
                                (from + direction * state.settings.holdDistance).position(),
                                from.rotation()
                            )
                        } else {
                            val hitPos = from.position() + direction * (snapDistance * result.hitFraction)

                            // the hit normal is facing from the surface, to the player
                            // but when holding (non-snap) it's the opposite direction
                            // so we invert the normal here to face it in the right direction
                            val dir = -result.hitNormal.alexandria()
                            val rotation = if (abs(dir.dot(Vector3.Up)) > 0.99) {
                                // `dir` and `up` are (close to) collinear
                                val yaw = player.handle.location.yaw.radians.toDouble()
                                // `rotation` will be facing "away" from the player
                                quaternionFromTo(Vector3.Forward, dir) *
                                    Euler3(z = (if (dir.y > 0.0) -yaw else yaw) + 2*PI).quaternion(EulerOrder.XYZ)
                            } else {
                                val v1 = Vector3.Up.cross(dir).normalized
                                val v2 = dir.cross(v1).normalized
                                quaternionOfAxes(v1, v2, dir)
                            }

                            Transform(hitPos, rotation)
                        } + state.settings.placeTransform
                    }

                    state.drawShape?.let { drawShape ->
                        CraftBulletAPI.drawOperationFor(drawShape, state.transform.bullet())
                            .invoke(CraftBulletAPI.drawableOf(player.handle, CraftBullet.DrawType.SHAPE))
                    }
                }

                state.parts.forEach { (mesh, transform) ->
                    mesh.transform = state.transform + transform
                }
            }
        }
    }

    override fun createFor(player: AlexandriaPlayer) = PlayerData(player)

    internal fun enable() {
        sokol.registerEvents(object : Listener {
            @EventHandler(priority = EventPriority.HIGH)
            fun on(event: PlayerInteractEvent) {
                if (event.hand != EquipmentSlot.HAND) return
                val player = event.player.alexandria
                val data = player.featureData(this@EntityHolding)
                data.state?.let { state ->
                    if (event.action.isLeftClick) {
                        exitInternal(player, state)
                        data.state = null
                        return
                    }

                    event.isCancelled = true
                    if (!event.action.isRightClick) return

                    // todo remove item here
                    sokol.entityHoster.hostMob(state.entity.toBlueprint(), player.handle.world, state.transform)
                }
            }

            @EventHandler(priority = EventPriority.HIGH)
            fun on(event: InventoryClickEvent) {
                val player = (event.whoClicked as? Player ?: return).alexandria
                player.featureData(this@EntityHolding).state?.let { state ->
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

val AlexandriaPlayer.entityHolding: EntityHolding.State?
    get() = featureData(SokolAPI.entityHolding).state
