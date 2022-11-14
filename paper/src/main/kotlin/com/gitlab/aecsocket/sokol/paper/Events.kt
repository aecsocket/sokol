package com.gitlab.aecsocket.sokol.paper

import com.github.retrooper.packetevents.event.PacketSendEvent
import com.gitlab.aecsocket.alexandria.core.input.Input
import com.gitlab.aecsocket.alexandria.paper.InputEvent
import com.gitlab.aecsocket.sokol.core.SokolEvent
import com.gitlab.aecsocket.sokol.core.SokolSpace
import com.gitlab.aecsocket.sokol.core.call
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent

object UpdateEvent : SokolEvent

fun SokolSpace.update() = call(UpdateEvent)

object ReloadEvent : SokolEvent

data class PlayerInputEvent(
    val player: Player,
    val input: Input,
    val cancel: () -> Unit,
) : SokolEvent {
    constructor(event: InputEvent) : this(event.player, event.input, event.cancel)
}

interface MobEvent : SokolEvent {
    object Spawn : SokolEvent

    object AddToWorld : SokolEvent

    object RemoveFromWorld : SokolEvent

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
    // as an alternative to listening to Create,
    // if we want to just make visual changes instead of a full item build
    // (e.g. just change custom model data, instead of building lore + writing persistence...)
    // useful for when we make a mesh of the item
    object CreateForm : ItemEvent

    object Create : ItemEvent

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
