package com.gitlab.aecsocket.sokol.paper

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent
import com.destroystokyo.paper.event.server.ServerTickStartEvent
import com.gitlab.aecsocket.alexandria.core.LogLevel
import com.gitlab.aecsocket.alexandria.core.input.Input
import com.gitlab.aecsocket.alexandria.paper.alexandria
import com.gitlab.aecsocket.sokol.core.PersistenceException
import com.gitlab.aecsocket.sokol.core.SokolEvent
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerDropItemEvent

internal class SokolEventListener(
    private val sokol: Sokol
) : Listener {
    private fun useEntity(event: Event, action: () -> Unit) {
        try {
            action()
        } catch (ex: PersistenceException) {
            sokol.log.line(LogLevel.Warning, ex) { "Could not handle event ${event::class}" }
        }
    }

    @EventHandler
    fun on(event: ServerTickStartEvent) {
        sokol.space.update()
    }

    @EventHandler
    fun on(event: EntityAddToWorldEvent) {
        val mob = event.entity
        if (sokol.mobsAdded.remove(mob.entityId)) {
            // if we've already called SokolEvent.Add on this newly-spawned mob...
            return
        }

        useEntity(event) {
            sokol.useMob(mob) { entity ->
                entity.call(SokolEvent.Add)
            }
        }
    }

    @EventHandler
    fun on(event: EntityRemoveFromWorldEvent) {
        val mob = event.entity

        sokol.entityHolding.heldBy[mob.uniqueId]?.let { state ->
            sokol.entityHolding.stop(state.player.alexandria)
        }
        useEntity(event) {
            sokol.useMob(mob) { entity ->
                entity.call(SokolEvent.Remove)
            }
        }
    }

    @EventHandler
    fun on(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val entityEvent = ItemEvent.Click(player, event)

        useEntity(event) {
            sokol.usePlayerItems(player) { entity ->
                entity.call(entityEvent)
            }

            event.currentItem?.let {
                sokol.useItem(it) { entity ->
                    entity.call(ItemEvent.ClickAsCurrent(player, event))
                }
            }

            event.cursor?.let {
                sokol.useItem(it) { entity ->
                    entity.call(ItemEvent.ClickAsCursor(player, event))
                }
            }
        }
    }
}
