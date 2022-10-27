package com.gitlab.aecsocket.sokol.paper

import com.github.retrooper.packetevents.event.*
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import com.gitlab.aecsocket.alexandria.core.LogLevel
import com.gitlab.aecsocket.sokol.core.PersistenceException
import org.bukkit.World
import org.bukkit.craftbukkit.v1_19_R1.CraftWorld
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import java.lang.ref.WeakReference

internal class SokolPacketListener(
    private val sokol: Sokol
) : PacketListenerAbstract(PacketListenerPriority.LOW) {
    // TODO: PacketEvents needs to update for Paper 1.19.2 new chunk logic
    // for now we will use a fallback method
    private val mobCache = HashMap<Int, WeakReference<Entity>>()

    private fun mobById(world: World, id: Int): Entity? {
        mobCache[id]?.let { ref -> ref.get()?.let { return it } }
        // avoids an async catch
        // this is an awful hack, but entities could be null if we're doing concurrent mod
        // too bad!
        val entities = (world as CraftWorld).handle.entityLookup.all.toList().filterNotNull()
        val mob = entities.firstOrNull { it.id == id }?.bukkitEntity
        mob?.let {
            mobCache[id] = WeakReference(it)
        }
        return mob
    }

    override fun onPacketSend(event: PacketSendEvent) {
        fun useEntity(action: () -> Unit) {
            try {
                action()
            } catch (ex: PersistenceException) {
                sokol.log.line(LogLevel.Warning, ex) { "Could not handle packet type ${event.packetType}" }
            }
        }

        val player = event.player as? Player ?: return
        when (event.packetType) {
            PacketType.Play.Server.SPAWN_ENTITY -> {
                val packet = WrapperPlayServerSpawnEntity(event)
                mobById(player.world, packet.entityId)?.let { mob ->
                    useEntity {
                        sokol.useMob(mob) { entity ->
                            entity.call(MobEvent.Show(player, event))
                        }
                    }
                }
            }
            PacketType.Play.Server.DESTROY_ENTITIES -> {
                val packet = WrapperPlayServerDestroyEntities(event)
                packet.entityIds.map { mobById(player.world, it) }.forEach { mob ->
                    mob?.let {
                        useEntity {
                            sokol.useMob(mob) { entity ->
                                entity.call(MobEvent.Hide(player, event))
                            }
                        }
                    }
                }
            }
        }
    }
}
