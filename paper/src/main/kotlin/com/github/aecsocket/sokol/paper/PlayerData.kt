package com.github.aecsocket.sokol.paper

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.wrapper.PacketWrapper
import org.bukkit.entity.Player

class PlayerData(plugin: SokolPlugin, val player: Player) {
    val effector = plugin.effectors.player(player)

    var showHosts: Boolean = false

    val rdTracking: MutableMap<Int, NodeRender.Tracking> = HashMap()
    var rdSelected: NodeRender.Selected? = null
    var rdShowShapes: NodeRender.ShowShape = NodeRender.ShowShape.NONE
}

fun Player.sendPacket(packet: PacketWrapper<*>) {
    PacketEvents.getAPI().playerManager.sendPacket(this, packet)
}

fun Player.sendPacketSilently(packet: PacketWrapper<*>) {
    PacketEvents.getAPI().playerManager.sendPacketSilently(this, packet)
}
