package com.github.aecsocket.sokol.paper

import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import io.github.retrooper.packetevents.util.SpigotReflectionUtil
import org.bukkit.entity.Player

internal class SokolPacketListener(
    private val plugin: SokolPlugin
) : PacketListenerAbstract() {
    override fun onPacketSend(event: PacketSendEvent) {
        val player = event.player
        if (player !is Player) return

        when (event.packetType) {
            PacketType.Play.Server.SPAWN_ENTITY -> {
                val packet = WrapperPlayServerSpawnEntity(event)
                (plugin.renders[packet.entityId] ?: SpigotReflectionUtil.getEntityById(packet.entityId)?.let {
                    plugin.persistence.getRender(it)
                })?.let { render ->
                    event.isCancelled = true
                    render.trackedBy(player)
                    plugin.playerData(player).trackingRenders[render.backing.entityId] = render
                }
            }
            PacketType.Play.Server.DESTROY_ENTITIES -> {
                val packet = WrapperPlayServerDestroyEntities(event)
                val tracking = plugin.playerData(player).trackingRenders
                packet.entityIds.forEach { tracking.remove(it) }
            }
        }
    }
}
