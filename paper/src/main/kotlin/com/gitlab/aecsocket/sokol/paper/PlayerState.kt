package com.gitlab.aecsocket.sokol.paper

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.wrapper.PacketWrapper
import net.kyori.adventure.extra.kotlin.join
import net.kyori.adventure.text.JoinConfiguration
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class PlayerState(private val plugin: Sokol, val player: Player) {
    val effector = plugin.effectors.player(player)
    val renders = DefaultNodeRenders.PlayerState(player, effector)
    var showHosts = false

    fun tick() {
        with(player) {
            val locale = locale()

            if (showHosts) {
                val lastHosts = plugin.lastHosts
                val possible = lastHosts.values.sumOf { it.possible }
                val marked = lastHosts.values.sumOf { it.marked }
                sendActionBar(plugin.i18n.safe(locale, "show_hosts") {
                    raw("marked") { marked }
                    raw("possible") { possible }
                    raw("percent") { marked.toDouble() / possible }
                    raw("mspt") { Bukkit.getAverageTickTime() }
                    raw("tps") { Bukkit.getTPS()[0] }
                }.join(JoinConfiguration.noSeparators()))
            }
        }
    }
}

fun Player.sendPacket(packet: PacketWrapper<*>) {
    PacketEvents.getAPI().playerManager.sendPacket(this, packet)
}

fun Player.sendPacketSilently(packet: PacketWrapper<*>) {
    PacketEvents.getAPI().playerManager.sendPacketSilently(this, packet)
}
