package com.github.aecsocket.sokol.paper

import net.kyori.adventure.extra.kotlin.join
import net.kyori.adventure.text.JoinConfiguration
import org.bukkit.Bukkit
import org.bukkit.entity.Player

internal data class PlayerData(
    private val plugin: SokolPlugin,
    private val player: Player
) {
    var showHosts: Boolean = false

    fun tick() {
        val locale = player.locale()
        if (showHosts) {
            val hosts = plugin.lastHosts
            val possible = hosts.values.sumOf { it.possible }
            val marked = hosts.values.sumOf { it.marked }
            player.sendActionBar(plugin.i18n.safe(locale, "show_hosts") {
                raw("marked") { marked }
                raw("possible") { possible }
                raw("percent") { marked.toDouble() / possible }
                raw("mspt") { Bukkit.getAverageTickTime() }
                raw("tps") { Bukkit.getTPS()[0] }
            }.join(JoinConfiguration.noSeparators()))
        }
    }
}
