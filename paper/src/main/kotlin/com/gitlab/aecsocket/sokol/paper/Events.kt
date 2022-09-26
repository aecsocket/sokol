package com.gitlab.aecsocket.sokol.paper

import com.github.retrooper.packetevents.event.PacketSendEvent
import com.gitlab.aecsocket.alexandria.paper.PacketInputListener
import com.gitlab.aecsocket.sokol.core.SokolEvent
import org.bukkit.event.player.PlayerDropItemEvent

interface MobEvent : SokolEvent {
    object Host : MobEvent

    data class Show(val backing: PacketSendEvent) : MobEvent
}

interface ItemEvent : SokolEvent {
    object Host : ItemEvent
}

interface PlayerEvent : SokolEvent {
    data class Input(val backing: PacketInputListener.Event) : PlayerEvent

    data class Drop(val backing: PlayerDropItemEvent) : PlayerEvent
}
