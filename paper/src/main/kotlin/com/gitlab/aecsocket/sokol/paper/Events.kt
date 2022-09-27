package com.gitlab.aecsocket.sokol.paper

import com.github.retrooper.packetevents.event.PacketSendEvent
import com.gitlab.aecsocket.alexandria.core.input.Input
import com.gitlab.aecsocket.sokol.core.SokolEvent
import org.bukkit.entity.Player

interface MobEvent : SokolEvent {
    object Host : MobEvent

    data class Show(val backing: PacketSendEvent) : MobEvent
}

interface ItemEvent : SokolEvent {
    object Host : ItemEvent
}

data class PlayerInput(
    val input: Input,
    val player: Player,
    val cancel: () -> Unit,
) : SokolEvent
