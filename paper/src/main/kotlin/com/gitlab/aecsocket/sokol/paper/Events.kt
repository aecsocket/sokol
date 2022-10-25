package com.gitlab.aecsocket.sokol.paper

import com.github.retrooper.packetevents.event.PacketSendEvent
import com.gitlab.aecsocket.alexandria.core.input.Input
import com.gitlab.aecsocket.sokol.core.SokolEvent
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent

interface MobEvent : SokolEvent {
    data class Show(
        val player: Player,
        val backing: PacketSendEvent,
    ) : MobEvent

    data class Hide(
        val player: Player,
        val backing: PacketSendEvent,
    ) : MobEvent
}

interface ItemEvent : SokolEvent {
    data class PlayerInput(
        val input: Input,
        val player: Player,
        val cancel: () -> Unit,
    ) : ItemEvent

    open class Click(
        val player: Player,
        val backing: InventoryClickEvent,
    ) : ItemEvent {
        val isLeftClick = backing.isLeftClick
        val isRightClick = backing.isRightClick
        val isShiftClick = backing.isShiftClick

        fun cancel() {
            backing.isCancelled = true
        }
    }

    class ClickAsCurrent(player: Player, backing: InventoryClickEvent) : Click(player, backing)

    class ClickAsCursor(player: Player, backing: InventoryClickEvent) : Click(player, backing)
}
