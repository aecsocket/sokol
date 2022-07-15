package com.github.aecsocket.sokol.paper

import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import org.bukkit.entity.Player

internal class SokolPacketListener(
    private val plugin: Sokol
) : PacketListenerAbstract() {
    override fun onPacketSend(event: PacketSendEvent) {
        val player = event.player
        if (player !is Player) return

        when (event.packetType) {
            PacketType.Play.Server.SPAWN_ENTITY -> {
                val packet = WrapperPlayServerSpawnEntity(event)
                if (plugin.renders.handleSpawnEntity(player, packet.entityId)) {
                    event.isCancelled = true
                }
            }
            PacketType.Play.Server.DESTROY_ENTITIES -> {
                val packet = WrapperPlayServerDestroyEntities(event)
                plugin.renders.handleDestroyEntities(player, packet.entityIds)
            }
        }
    }
}
