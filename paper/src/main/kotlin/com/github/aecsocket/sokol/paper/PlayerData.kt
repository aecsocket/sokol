package com.github.aecsocket.sokol.paper

import com.github.aecsocket.alexandria.core.LogLevel
import com.github.aecsocket.alexandria.core.effect.ParticleEffect
import com.github.aecsocket.alexandria.core.extension.*
import com.github.aecsocket.alexandria.core.physics.*
import com.github.aecsocket.alexandria.core.spatial.Quaternion
import com.github.aecsocket.alexandria.core.spatial.Transform
import com.github.aecsocket.alexandria.core.spatial.Vector3
import com.github.aecsocket.alexandria.paper.extension.alexandria
import com.github.aecsocket.alexandria.paper.extension.bukkitCurrentTick
import com.github.aecsocket.alexandria.paper.extension.bukkitNextEntityId
import com.github.aecsocket.alexandria.paper.extension.vector
import com.github.aecsocket.sokol.paper.feature.InspectFeature
import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.protocol.player.Equipment
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot
import com.github.retrooper.packetevents.util.Vector3d
import com.github.retrooper.packetevents.util.Vector3f
import com.github.retrooper.packetevents.wrapper.PacketWrapper
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRelativeMove
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import net.kyori.adventure.extra.kotlin.join
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component.empty
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.title.Title.Times.times
import net.kyori.adventure.title.Title.title
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required
import java.time.Duration.ZERO
import java.time.Duration.ofMillis
import java.util.*
import kotlin.collections.ArrayList

interface InspectRender {
    fun select()

    fun deselect()

    fun moveTo(pos: Vector3)

    fun rotateTo(rot: Quaternion)

    fun remove()
}

/*
transform hierarchy:
  InspectPart(gun_receiver): Transform[tl = (500.0, 64.0, 500.0)]
   -> InspectPart(gun_stock): Transform[tl = (0.0, 0.0, -0.5)]
   -> etc.`
 */
data class InspectSlot(
    val transform: Transform,
    var part: InspectPart? = null
)

@ConfigSerializable
data class InspectDrag(
    @Required val direction: Vector3,
    @Required val distance: Double,
)

data class InspectPart(
    val node: PaperDataNode,
    val bodies: List<SimpleBody>,
    val render: InspectRender,
    val slots: Map<String, InspectSlot>,
    val attachedTransform: Transform,
    val drag: InspectDrag?,
    val invAttachedTransform: Transform = attachedTransform.inverse,
) {
    fun remove() {
        render.remove()
        slots.forEach { (_, slot) -> slot.part?.remove() }
    }
}

data class InspectView(
    val root: InspectPart,
    var transform: Transform,
)

data class InspectSelection(
    val part: InspectPart,
    var dragging: Boolean = false,
    var dragTransform: Transform = Transform.Identity
)

data class PlayerData(
    private val plugin: SokolPlugin,
    val player: Player
) {
    inner class EntityInspectRender(
        val entityId: Int,
        val origin: Vector3,
    ) : InspectRender {
        private var lastPos = origin

        private fun glowing(value: Boolean) {
            sendPacket(WrapperPlayServerEntityMetadata(entityId, listOf(
                EntityData(0, EntityDataTypes.BYTE, // generic
                    (0x20 or (if (value) 0x40 else 0)).toByte()) // invisible + (glowing?)
            )))
        }

        override fun select() = glowing(true)
        override fun deselect() = glowing(false)

        override fun moveTo(pos: Vector3) {
            val (x, y, z) = pos - lastPos
            sendPacket(WrapperPlayServerEntityRelativeMove(entityId,
                x, y, z, true))
            lastPos = pos
        }

        override fun rotateTo(rot: Quaternion) {
            val angle = rot.euler().y { -it }.z { -it }.degrees
            val (x, y, z) = angle
            sendPacket(WrapperPlayServerEntityMetadata(entityId, listOf(
                EntityData(16, EntityDataTypes.ROTATION,
                    Vector3f(x.toFloat(), y.toFloat(), z.toFloat())), // head pose
            )))
        }

        override fun remove() {
            sendPacket(WrapperPlayServerDestroyEntities(entityId))
        }
    }

    val effector = plugin.effectors.player(player)

    var showHosts: Boolean = false

    private val _inspectViews = ArrayList<InspectView>()
    val isViews: List<InspectView> get() = _inspectViews
    var isShowShapes = false
    var isRotation: Quaternion? = null
    var isSelection: InspectSelection? = null

    fun destroy() {
        _inspectViews.forEach { it.root.remove() }
    }

    private fun sendPacket(packet: PacketWrapper<*>) {
        PacketEvents.getAPI().playerManager.sendPacket(player, packet)
    }

    fun addInspectView(root: InspectPart, transform: Transform): InspectView {
        return InspectView(root, transform).also { _inspectViews.add(it) }
    }

    fun addInspectView(root: PaperDataNode, transform: Transform): InspectView {
        val rot = (-transform.rot.euler().degrees).run {
            Vector3f(x.toFloat(), y.toFloat(), z.toFloat())
        }
        fun renderOf(pos: Vector3, node: PaperDataNode): InspectRender {
            val entityId = bukkitNextEntityId
            sendPacket(WrapperPlayServerSpawnEntity(entityId, Optional.of(UUID.randomUUID()), EntityTypes.ARMOR_STAND,
                Vector3d(pos.x, pos.y - 1.45, pos.z), 0f, 0f, 0f, 0, Optional.empty()))

            sendPacket(WrapperPlayServerEntityMetadata(entityId, listOf(
                EntityData(0, EntityDataTypes.BYTE, (0x20).toByte()), // invisible
                EntityData(15, EntityDataTypes.BYTE, (0x10).toByte()), // marker
                EntityData(16, EntityDataTypes.ROTATION, rot), // head pose
            )))

            val stack = try {
                // remove children, so only this one component is rendered out
                // (e.g. if this was the root of a gun tree, and the gun changed model when it was
                //  complete, we *don't* want that)
                plugin.persistence.nodeToStack(node.copy().apply { removeChildren() })
            } catch (ex: NodeItemCreationException) {
                plugin.log.line(LogLevel.Warning, ex) { "Could not make stack" } // todo
                throw ex
            }

            sendPacket(WrapperPlayServerEntityEquipment(entityId, listOf(
                Equipment(EquipmentSlot.HELMET, SpigotConversionUtil.fromBukkitItemStack(stack))
            )))

            return EntityInspectRender(entityId, pos)
        }

        fun partOf(node: PaperDataNode, tf: Transform, isRoot: Boolean = false): InspectPart {
            val feature = node.component.features[InspectFeature.ID]?.let {
                it as InspectFeature.Profile
            } ?: throw IllegalStateException() // todo

            val slots = node.component.slots.map { (key, _) ->
                val slotTf = feature.slotTransforms[key]
                    ?: throw IllegalStateException("No slot transform for ${node.component.id}:$key at ${node.path()}") // todo
                key to InspectSlot(slotTf, node.node(key)?.let { child ->
                    partOf(child, tf + slotTf)
                })
            }.associate { it }

            // only combine attachedTransform if the node is non-root
            val posTf = if (isRoot) tf else tf + feature.invAttachedTransform

            return InspectPart(
                node, feature.bodies, renderOf(posTf.apply(), node), slots,
                feature.attachedTransform, feature.drag, feature.invAttachedTransform
            )
        }

        return addInspectView(partOf(root, transform, true), transform)
    }

    fun removeInspectView(view: InspectView) {
        _inspectViews.remove(view)
    }

    fun tick() {
        with(player) {
            val locale = locale()

            val pt = Array<Vector3>(10) { Vector3(0.1 * it, 0.0, 0.0) }
            val qt = Transform(
                rot = Euler3(0.0, 45.0, 45.0).radians.quaternion())
            effector.showParticle(ParticleEffect(Key.key("minecraft:flame")), Vector3(0.0))

            isRotation?.let { isRot ->
                pt.forEach { ptt ->
                    effector.showParticle(plugin.settings.inspectView.pointParticle!!, ptt)
                    effector.showParticle(plugin.settings.inspectView.shapeParticle!!, isRot * ptt)
                }

                sendActionBar(text("rot = $isRot = ${isRot.euler().degrees}"))
            }

            if (showHosts) {
                val hosts = plugin.lastHosts
                val possible = hosts.values.sumOf { it.possible }
                val marked = hosts.values.sumOf { it.marked }
                sendActionBar(plugin.i18n.safe(locale, "show_hosts") {
                    raw("marked") { marked }
                    raw("possible") { possible }
                    raw("percent") { marked.toDouble() / possible }
                    raw("mspt") { Bukkit.getAverageTickTime() }
                    raw("tps") { Bukkit.getTPS()[0] }
                }.join(JoinConfiguration.noSeparators()))
            }

            /*val raycast = PaperRaycast(world)
            raycast.cast(Ray(eyeLocation.vector(), location.direction.alexandria()), 32.0) { when (it) {
                is PaperEntityBody -> it.entity != player
                is PaperBlockBody -> it.fluid == null
            } }?.let { col ->
                val hit = col.hit
                sendActionBar(text("[hit] %.2f @ ${col.hit}".format(col.tIn)))
                effector.showLine(ParticleEffect(key("minecraft:bubble_pop")), col.posIn, col.posIn + col.normal * 2.0, 0.1)
                val shape = hit.shape
                if (shape is Box) {
                    effector.showCuboid(ParticleEffect(key("minecraft:bubble")), verticesOf(shape).map { hit.transform.apply(it) }, 0.1)
                }
            } ?: run {
                sendActionBar(text("[no hit]"))
            }*/

            data class CastInspectBody(
                override val backing: Body,
                val part: InspectPart
            ) : ForwardingBody

            val allBodies = ArrayList<CastInspectBody>()
            _inspectViews.forEach { view ->
                isRotation?.let { rot ->
                    view.transform = view.transform.copy(rot = rot)
                }

                fun apply(part: InspectPart, originTf: Transform, isRoot: Boolean = false) {
                    val selected = isSelection?.let { if (it.part == part) it else null }
                    val tf = selected?.let {
                        if (it.dragging) originTf + it.dragTransform else originTf
                    } ?: originTf

                    if (isShowShapes && isRoot) {
                        plugin.settings.inspectView.pointParticle?.let {
                            effector.showParticle(it, (tf + part.attachedTransform).apply())
                        }
                    }

                    part.bodies.forEach { body ->
                        if (isShowShapes) {
                            plugin.settings.inspectView.shapeParticle?.let {
                                effector.showShape(
                                    it,
                                    body.shape,
                                    tf + body.transform,
                                    plugin.settings.inspectView.shapeStep
                                )
                            }
                        }

                        allBodies.add(CastInspectBody(
                            body.copy(transform = tf + body.transform), part
                        ))
                    }

                    part.slots.forEach { (_, slot) ->
                        if (isShowShapes) {
                            plugin.settings.inspectView.pointParticle?.let {
                                effector.showParticle(it, (tf + slot.transform).apply())
                            }
                        }

                        slot.part?.let { part ->
                            apply(part, tf + slot.transform + part.invAttachedTransform)
                        }
                    }

                    part.render.moveTo(tf.apply())
                    part.render.rotateTo(tf.rot)
                }

                apply(view.root, view.transform, true)
            }

            // cannot change selection while dragging
            if (isSelection == null || isSelection?.dragging == false) {
                fun InspectSelection.deselect() {
                    part.render.deselect()
                }

                raycastOf(allBodies).cast(Ray(eyeLocation.vector(), location.direction.alexandria()), 8.0)?.let { col ->
                    val hit = col.hit
                    val part = hit.part

                    fun select() {
                        part.render.select()
                        isSelection = InspectSelection(part)
                    }

                    isSelection?.let {
                        if (part != it.part) {
                            isSelection?.deselect()
                            select()
                        }
                    } ?: select()
                } ?: run {
                    isSelection?.deselect()
                    isSelection = null
                }
            }

            isSelection?.let { selection ->
                val part = selection.part
                val dragging = selection.dragging
                showTitle(title(
                    empty(),
                    text()
                        .append(text(" ".repeat(20)))
                        .append(part.node.component.localize(plugin.i18n).join())
                        .append(text(if (dragging) " *" else ""))
                        .build(),
                    times(ZERO, ofMillis(200), ZERO)
                ))

                if (dragging) {
                    part.drag?.let { drag ->
                        selection.dragTransform = Transform(
                            tl = drag.direction * drag.distance
                        )
                    }
                }
            }
        }

        //isRotation = null
    }
}
