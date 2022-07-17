package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.core.Input
import com.gitlab.aecsocket.sokol.core.event.NodeEvent
import org.bukkit.entity.Player

interface PaperNodeEvent : NodeEvent {
    object OnTick : NodeEvent.OnTick

    data class OnInput(
        override val input: Input,
        val player: Player,
    ) : NodeEvent.OnInput
}
