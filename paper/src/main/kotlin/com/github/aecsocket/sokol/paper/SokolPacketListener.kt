package com.github.aecsocket.sokol.paper

import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketReceiveEvent

internal class SokolPacketListener(
    private val plugin: SokolPlugin
) : PacketListenerAbstract() {
    override fun onPacketReceive(event: PacketReceiveEvent) {

    }
}
