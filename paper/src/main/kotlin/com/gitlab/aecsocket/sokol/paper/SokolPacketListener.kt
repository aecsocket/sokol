package com.gitlab.aecsocket.sokol.paper

import com.github.retrooper.packetevents.event.*
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import io.github.retrooper.packetevents.util.SpigotReflectionUtil
import org.bukkit.entity.Player

internal class SokolPacketListener(
    private val sokol: Sokol
) : PacketListenerAbstract(PacketListenerPriority.LOW) {
    override fun onPacketSend(event: PacketSendEvent) {
        if (event.player !is Player) return
        when (event.packetType) {
            PacketType.Play.Server.SPAWN_ENTITY -> {
                val packet = WrapperPlayServerSpawnEntity(event)
                SpigotReflectionUtil.getEntityById(packet.entityId)?.let { mob ->
                    sokol.useMob(mob) { entity ->
                        entity.call(MobEvent.Show(event))
                    }
                }
            }
        }
    }
}
