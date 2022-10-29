package com.gitlab.aecsocket.sokol.paper

import com.github.retrooper.packetevents.event.PacketSendEvent
import com.gitlab.aecsocket.alexandria.core.input.Input
import com.gitlab.aecsocket.craftbullet.core.BlockRigidBody
import com.gitlab.aecsocket.craftbullet.core.TrackedPhysicsObject
import com.gitlab.aecsocket.sokol.core.SokolEvent
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

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
    // as an alternative to listening to SokolEvent.Add,
    // if we want to just make visual changes instead of a full item build
    // (e.g. just change custom model data, instead of building lore + writing persistence...)
    // useful for when we make a mesh of the item
    data class CreateForm(
        val item: ItemStack,
        val meta: ItemMeta
    ) : ItemEvent

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

data class PlayerInput(
    val input: Input,
    val player: Player,
    val cancel: () -> Unit,
) : SokolEvent
