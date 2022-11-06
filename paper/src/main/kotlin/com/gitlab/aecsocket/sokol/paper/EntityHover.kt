package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.paper.AlexandriaPlayer
import com.gitlab.aecsocket.alexandria.paper.PlayerFeature
import com.gitlab.aecsocket.alexandria.paper.alexandria
import com.gitlab.aecsocket.alexandria.paper.extension.bukkitPlayers
import com.gitlab.aecsocket.alexandria.paper.extension.registerEvents
import com.gitlab.aecsocket.alexandria.paper.extension.scheduleDelayed
import com.gitlab.aecsocket.craftbullet.paper.CraftBulletAPI
import com.gitlab.aecsocket.craftbullet.paper.rayTestFrom
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.component.Hovered
import com.gitlab.aecsocket.sokol.paper.component.SokolPhysicsObject
import com.gitlab.aecsocket.sokol.paper.component.SupplierEntityAccess
import com.gitlab.aecsocket.sokol.paper.component.hovered
import com.jme3.bullet.collision.PhysicsRayTestResult
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class EntityHover internal constructor(
    private val sokol: Sokol
) : PlayerFeature<EntityHover.PlayerData> {
    data class HoverData(
        val obj: SokolPhysicsObject,
        val rayTestResult: PhysicsRayTestResult,
    )

    inner class PlayerData(
        private val player: AlexandriaPlayer,
        var hover: HoverData? = null
    ) : PlayerFeature.PlayerData {
        override fun update() {
            if (player.heldEntity != null) return

            CraftBulletAPI.executePhysics {
                val hover = hover
                val newHover: HoverData? = player.handle
                    .rayTestFrom(sokol.settings.entityHoverDistance)
                    .firstOrNull()?.let { result ->
                        val obj = result.collisionObject
                        if (obj is SokolPhysicsObject) {
                            HoverData(obj, result)
                        } else null
                    }

                fun call(obj: SokolPhysicsObject, event: SokolEvent) {
                    obj.entity.call(event)
                }

                hover?.let {
                    if (newHover == null || newHover.obj !== hover.obj) {
                        call(hover.obj, HoverState(player.handle, false, hover.rayTestResult))
                    }
                }
                newHover?.let {
                    if (hover == null || hover.obj !== newHover.obj) {
                        call(newHover.obj, HoverState(player.handle, true, newHover.rayTestResult))
                    } else {
                        val oldIndex = hover.rayTestResult.triangleIndex()
                        val newIndex = newHover.rayTestResult.triangleIndex()
                        if (oldIndex != newIndex) {
                            call(hover.obj, ChangeHoverIndex(player.handle, oldIndex, newIndex))
                        }
                    }
                }
                this.hover = newHover
            }
        }
    }

    private lateinit var mSupplierEntityAccess: ComponentMapper<SupplierEntityAccess>
    private lateinit var mHovered: ComponentMapper<Hovered>

    override fun createFor(player: AlexandriaPlayer) = PlayerData(player)

    internal fun enable() {
        mSupplierEntityAccess = sokol.engine.componentMapper()
        mHovered = sokol.engine.componentMapper()

        sokol.inputHandler { event ->
            val player = event.player
            val axPlayer = player.alexandria

            // if the player's holding an entity, it gets the event precedence over the hovered
            axPlayer.heldEntity?.entity?.let {
                /*mSupplierEntityAccess.getOr(it)?.useEntity { entity ->
                    entity.call(event)
                }*/
                it.call(event)
            } ?: run {
                axPlayer.hoveredEntity?.let { (obj, rayTestResult) ->
                    mSupplierEntityAccess.getOr(obj.entity)?.useEntity { entity ->
                        mHovered.set(entity, hovered(event.player, rayTestResult))
                        entity.call(event)
                    }
                }
            }
        }

        sokol.registerEvents(object : Listener {
            @EventHandler
            fun on(event: PlayerJoinEvent) {
                event.player.alexandria.featureData(this@EntityHover)
            }
        })

        // if after a /reload
        bukkitPlayers.forEach { player ->
            player.alexandria.featureData(this@EntityHover)
        }
    }

    data class HoverState(
        val player: Player,
        val hovered: Boolean,
        val testResult: PhysicsRayTestResult
    ) : SokolEvent

    data class ChangeHoverIndex(
        val player: Player,
        val oldIndex: Int,
        val newIndex: Int
    ) : SokolEvent
}

val AlexandriaPlayer.hoveredEntity: EntityHover.HoverData?
    get() = featureData(SokolAPI.entityHover).hover
