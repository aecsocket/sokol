package com.gitlab.aecsocket.sokol.paper

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.world.EntitiesUnloadEvent

internal class SokolListener(
    private val plugin: Sokol
) : Listener {
    @EventHandler
    fun PlayerJoinEvent.on() {
        plugin.playerState(player)
    }

    @EventHandler
    fun PlayerQuitEvent.on() {
        plugin.removePlayerState(player)
    }

    @EventHandler
    fun EntityRemoveFromWorldEvent.on() {
        plugin.renders.remove(entity.entityId)
    }

    @EventHandler
    fun EntitiesUnloadEvent.on() {
        entities.forEach { plugin.renders.remove(it.entityId) }
    }

    @EventHandler
    fun InventoryClickEvent.on() {
        val player = whoClicked as? Player ?: return
        currentItem?.let { stack ->
            val playerHost = plugin.hostOf(whoClicked)
            plugin.persistence.useStack(stack,
                // if the clicked node is in the top inventory, we don't *really* know what we're dealing with
                // GUI inventory? player inspect view? chest inventory?
                // TODO maybe revisit this
                if (clickedInventory == view.topInventory) holderByContainer(playerHost, slot)
                else holderByPlayer(playerHost, player, slot)
            ) { state, host ->
                state.callEvent(host, PaperNodeEvent.OnClickAsCurrent(isLeftClick, isRightClick, isShiftClick, this))
            }
        }
    }
}
