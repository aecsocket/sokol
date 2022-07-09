package com.github.aecsocket.sokol.paper

import com.github.aecsocket.alexandria.core.keyed.by
import com.github.aecsocket.alexandria.core.physics.Quaternion
import com.github.aecsocket.alexandria.core.physics.Transform
import com.github.aecsocket.alexandria.paper.extension.bukkitNextEntityId
import com.github.aecsocket.alexandria.paper.extension.transform
import com.github.aecsocket.sokol.core.errorMsg
import com.github.aecsocket.sokol.core.feature.ItemHostFeature
import com.github.aecsocket.sokol.core.feature.RenderFeature
import com.github.aecsocket.sokol.core.util.RenderMesh
import com.github.aecsocket.sokol.paper.extension.asStack
import com.github.aecsocket.sokol.paper.feature.PaperItemHost
import com.github.aecsocket.sokol.paper.feature.PaperRender
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.protocol.player.Equipment
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot
import com.github.retrooper.packetevents.util.Vector3d
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*
import kotlin.collections.HashMap

class NodeRenders internal constructor(
    private val plugin: SokolPlugin
) {
    inner class SpawnedMesh(
        val backing: RenderMesh,
        val entityId: Int,
        val uuid: UUID,
        val item: ItemStack,
    ) {
        fun spawn(player: Player, baseTf: Transform) {
            val tf = baseTf + backing.transform
            val (x, y, z) = tf.apply()

            player.sendPacket(WrapperPlayServerSpawnEntity(
                entityId,
                Optional.of(uuid),
                EntityTypes.ARMOR_STAND,
                Vector3d(x, y - 1.45, z),
                0f, 0f, 0f, 0, Optional.empty(),
            ))

            player.sendPacket(WrapperPlayServerEntityMetadata(entityId, listOf(
                EntityData(0, EntityDataTypes.BYTE, (0x20).toByte()), // invisible
                EntityData(15, EntityDataTypes.BYTE, (0x10).toByte()), // marker
            )))

            player.sendPacket(WrapperPlayServerEntityEquipment(entityId, listOf(
                Equipment(EquipmentSlot.HELMET, SpigotConversionUtil.fromBukkitItemStack(item))
            )))
        }
    }

    fun spawn(mesh: RenderMesh, node: PaperDataNode): SpawnedMesh {
        return SpawnedMesh(mesh, bukkitNextEntityId, UUID.randomUUID(), when (mesh) {
            is RenderMesh.Static -> mesh.item
            is RenderMesh.Dynamic -> {
                val state = paperStateOf(node)
                (state.nodeStates[node]?.by<PaperItemHost.Profile.State>(ItemHostFeature)
                    ?: throw IllegalStateException(node.errorMsg("No feature '${ItemHostFeature.id}' for dynamic mesh"))
                ).itemDescriptor(state)
            }
        }.asStack())
    }

    data class Slot(
        val transform: Transform,
        var part: Part? = null,
    )

    data class Part(
        val node: PaperDataNode,
        val meshes: Collection<SpawnedMesh>,
        val asChild: Transform,
        val slots: Map<String, Slot>,
    )

    fun partOf(node: PaperDataNode): Part {
        val render = node.component.features.by<PaperRender.Profile>(RenderFeature)
            ?: throw IllegalStateException(node.errorMsg("No feature '${RenderFeature.id}'"))
        return Part(
            node,
            render.meshes.map { spawn(it, node) },
            render.asChild.inverse,
            node.component.slots.map { (key, _) ->
                key to Slot(
                    render.slots[key]
                        ?: throw IllegalStateException(node.errorMsg("No slot transform for slot '$key'")),
                    node.node(key)?.let { partOf(it) }
                )
            }.associate { it }
        )
    }

    inner class State(
        val backing: Entity,
        var rotation: Quaternion,
        val root: Part,
    ) {
        var transform: Transform
            get() = backing.transform.copy(rot = rotation)
            set(value) {
                backing.transform = value
                rotation = value.rot
            }

        fun trackedBy(player: Player) {
            fun apply(part: Part, baseTf: Transform) {
                val tf = baseTf

                part.meshes.forEach { mesh ->
                    mesh.spawn(player, tf)
                }

                part.slots.forEach { (_, slot) ->
                    slot.part?.let { apply(it, tf + slot.transform + it.asChild) }
                }
            }

            apply(root, transform)
        }

        fun trackedBy() = plugin.playerData.filter { (_, data) -> data.trackingRenders.containsKey(backing.entityId) }

        internal fun tick() {

        }
    }

    private val _entries = HashMap<Int, State>()
    val entries: Map<Int, State> get() = _entries

    operator fun get(id: Int) = _entries[id]

    fun create(backing: Entity, node: PaperDataNode, rotation: Quaternion): State {
        return State(backing, rotation, partOf(node)).also { _entries[backing.entityId] = it }
    }

    fun remove(id: Int) = _entries.remove(id)

    internal fun tick() {
        _entries.forEach { (_, state) -> state.tick() }
    }
}
