package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.core.Input
import com.gitlab.aecsocket.sokol.core.event.NodeEvent
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent

interface PaperNodeEvent : NodeEvent {
    object OnTick : NodeEvent.OnTick

    object OnHosted : NodeEvent.OnHosted

    object OnHostUpdate : NodeEvent.OnHostUpdate

    data class OnInput(
        override val input: Input,
        val player: Player,
    ) : NodeEvent.OnInput

    data class OnClickAsCurrent(
        override val leftClick: Boolean,
        override val rightClick: Boolean,
        override val shiftClick: Boolean,
        val backing: InventoryClickEvent,
    ) : NodeEvent.OnClickAsCurrent
}
