package com.gitlab.aecsocket.sokol.paper

import com.github.retrooper.packetevents.event.PacketSendEvent
import com.gitlab.aecsocket.sokol.core.SokolEvent

interface EntityEvent : SokolEvent {
    object Host : EntityEvent

    data class Show(val backing: PacketSendEvent): EntityEvent
}
