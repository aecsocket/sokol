package com.github.aecsocket.sokol.paper

import com.github.aecsocket.alexandria.core.Input
import com.github.aecsocket.sokol.core.event.NodeEvent
import org.bukkit.entity.Player

interface PaperNodeEvent : NodeEvent {
    object OnTick : NodeEvent.OnTick

    data class OnInput(
        override val input: Input,
        val player: Player,
    ) : NodeEvent.OnInput
}
