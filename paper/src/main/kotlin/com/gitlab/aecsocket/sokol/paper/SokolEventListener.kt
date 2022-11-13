package com.gitlab.aecsocket.sokol.paper

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent
import com.destroystokyo.paper.event.server.ServerTickStartEvent
import com.gitlab.aecsocket.alexandria.core.LogLevel
import com.gitlab.aecsocket.sokol.core.*
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent

internal class SokolEventListener(
    private val sokol: Sokol
) : Listener {
    private fun tryEvent(event: Event, block: () -> Unit) {
        try {
            return block()
        } catch (ex: Exception) {
            sokol.log.line(LogLevel.Warning, ex) { "Could not handle event ${event::class}" }
        }
    }

    @EventHandler
    fun on(event: ServerTickStartEvent) {
        tryEvent(event) {
            sokol.space.update()
        }
    }

    @EventHandler
    fun on(event: EntityAddToWorldEvent) {
        val mob = event.entity
        if (sokol.mobsAdded.remove(mob.entityId)) {
            // if we've already called Add on this newly-spawned mob...
            return
        }

        tryEvent(event) {
            sokol.useSpace { space ->
                sokol.resolver.readMob(mob, space)
                space.construct()
                space.call(AddToWorldEvent)
            }
        }
    }

    @EventHandler
    fun on(event: EntityRemoveFromWorldEvent) {
        val mob = event.entity

        tryEvent(event) {
            sokol.useSpace { space ->
                sokol.resolver.readMob(mob, space)
                space.construct()
                space.call(RemoveFromWorldEvent)
            }
        }
    }

    @EventHandler
    fun on(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return

        tryEvent(event) {
            sokol.useSpace { space ->
                sokol.resolver.readPlayerItems(player, space)
                space.construct()
                space.call(ItemEvent.Click(player, event))
            }

            event.currentItem?.let {
                sokol.useSpace { space ->
                    sokol.resolver.readItem(it, space)
                    space.construct()
                    space.call(ItemEvent.ClickAsCurrent(player, event))
                }
            }

            event.cursor?.let {
                sokol.useSpace { space ->
                    sokol.resolver.readItem(it, space)
                    space.construct()
                    space.call(ItemEvent.ClickAsCursor(player, event))
                }
            }
        }
    }
}
