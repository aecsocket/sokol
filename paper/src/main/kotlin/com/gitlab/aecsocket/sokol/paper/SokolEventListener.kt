package com.gitlab.aecsocket.sokol.paper

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent
import com.gitlab.aecsocket.alexandria.core.input.Input
import com.gitlab.aecsocket.sokol.core.SokolEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerDropItemEvent

internal class SokolEventListener(
    private val sokol: Sokol
) : Listener {
    @EventHandler
    fun on(event: EntityAddToWorldEvent) {
        val mob = event.entity
        sokol.useMob(mob) { entity ->
            entity.call(SokolEvent.Add)
        }
    }

    @EventHandler
    fun on(event: EntityRemoveFromWorldEvent) {
        val mob = event.entity
        sokol.useMob(mob) { entity ->
            entity.call(SokolEvent.Remove)
        }
    }

    @EventHandler
    fun on(event: PlayerDropItemEvent) {
        val player = event.player
        sokol.usePlayerItems(event.player) { entity ->
            entity.call(PlayerInput(Input.Drop, player) { event.isCancelled = true })
        }
    }
}
