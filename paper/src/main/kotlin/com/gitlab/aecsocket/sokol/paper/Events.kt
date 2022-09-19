package com.gitlab.aecsocket.sokol.paper

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.gitlab.aecsocket.sokol.core.SokolEvent

interface ByEntityEvent : SokolEvent {
    data class Added(val backing: EntityAddToWorldEvent) : ByEntityEvent

    data class Removed(val backing: EntityRemoveFromWorldEvent) : ByEntityEvent

    data class Shown(val backing: PacketSendEvent): ByEntityEvent

    data class Hidden(val backing: PacketSendEvent, var thisEntityCancelled: Boolean = false): ByEntityEvent
}
