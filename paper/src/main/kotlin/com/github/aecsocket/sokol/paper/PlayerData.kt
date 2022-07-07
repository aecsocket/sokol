package com.github.aecsocket.sokol.paper

import com.github.aecsocket.alexandria.core.bound.*
import com.github.aecsocket.alexandria.core.effect.ParticleEffect
import com.github.aecsocket.alexandria.core.extension.*
import com.github.aecsocket.alexandria.core.spatial.Vector3
import com.github.aecsocket.alexandria.paper.extension.alexandria
import com.github.aecsocket.alexandria.paper.extension.plus
import com.github.aecsocket.alexandria.paper.extension.times
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
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.bossbar.BossBar.bossBar
import net.kyori.adventure.extra.kotlin.join
import net.kyori.adventure.key.Key.key
import net.kyori.adventure.text.Component.empty
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.format.NamedTextColor.*
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title.Times.times
import net.kyori.adventure.title.Title.title
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.time.Duration.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.sin

@JvmInline
value class InspectRender(val entityId: Int)

data class InspectComponent(
    val node: PaperDataNode,
    val position: Vector3,
    val bound: Bound,
    val render: InspectRender,
)

data class InspectState(
    val tree: PaperTreeState,
    val infoBar: BossBar,
    val origin: Vector3,
    val components: List<InspectComponent>,
    var selected: InspectComponent? = null,
)

data class PlayerData(
    private val plugin: SokolPlugin,
    val player: Player
) {
    var showHosts: Boolean = false
    var inspectState: InspectState? = null

    val locale: Locale get() = player.locale()

    fun destroy() {
        exitInspectState()
    }

    private fun InspectRender.glowing(value: Boolean) {
        PacketEvents.getAPI().playerManager.sendPacket(player, WrapperPlayServerEntityMetadata(
            entityId, listOf(EntityData(0, EntityDataTypes.BYTE,
                (0x20 or (if (value) 0x40 else 0)).toByte())) // invisible + glowing?
        ))
    }

    fun InspectRender.select() {
        glowing(true)
    }

    fun InspectRender.deselect() {
        glowing(false)
    }

    fun InspectRender.rotate(angles: Euler3) {
        PacketEvents.getAPI().playerManager.sendPacket(player, WrapperPlayServerEntityMetadata(
            entityId, listOf(EntityData(16, EntityDataTypes.ROTATION,
                Vector3f(angles.pitch.toFloat(), angles.yaw.toFloat(), angles.roll.toFloat()))) // head rotation
        ))
    }

    fun enterInspectState(tree: PaperTreeState) {
        exitInspectState()
        val root = tree.root
        val locale = this.locale

        val infoBar = bossBar(plugin.i18n.safe(locale, "inspect_info") {
            list("name") {
                root.component.localize(this).forEach { sub(it) }
            }
        }.join(), 1f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS)
        player.showBossBar(infoBar)

        val origin = (player.eyeLocation + (player.location.direction * 2.0)).vector()

        val components = ArrayList<InspectComponent>()

        fun makeComponents(node: PaperDataNode, position: Vector3) {
            val feature = node.component.features[InspectFeature.ID]?.let {
                it as InspectFeature.Profile
            } ?: throw IllegalStateException("No inspect feature for ${node.path()}")
            val stack = plugin.persistence.nodeToStack(node) // we only generate the stack for the subtree

            @Suppress("DEPRECATION")
            val entityId = Bukkit.getUnsafe().nextEntityId()
            PacketEvents.getAPI().playerManager.apply {
                val entityPos = origin + position
                sendPacket(player, WrapperPlayServerSpawnEntity(
                    entityId, Optional.of(UUID.randomUUID()), EntityTypes.ARMOR_STAND,
                    Vector3d(entityPos.x, entityPos.y - 1.45, entityPos.z), 0f, 0f, 0f, 0, Optional.empty(),
                ))

                sendPacket(player, WrapperPlayServerEntityMetadata(
                    entityId, listOf(
                        EntityData(0, EntityDataTypes.BYTE, (0x20).toByte()), // invisible
                        EntityData(15, EntityDataTypes.BYTE, (0x10).toByte()), // marker
                    )
                ))

                sendPacket(player, WrapperPlayServerEntityEquipment(
                    entityId, listOf(
                        Equipment(EquipmentSlot.HELMET, SpigotConversionUtil.fromBukkitItemStack(stack))
                    )
                ))
            }

            components.add(InspectComponent(
                node, position, feature.bound, InspectRender(entityId)
            ))

            node.children.forEach { (key, child) ->
                val offset = feature.slotOffsets[key] ?: throw IllegalStateException("No slot offset for ${node.path()} : $key")
                makeComponents(child, position + offset) // TODO this doesnt take into account rotations
            }
        }

        makeComponents(root, Vector3.Zero)

        inspectState = InspectState(tree, infoBar, origin, components)
    }

    fun exitInspectState() {
        inspectState?.apply {
            player.hideBossBar(infoBar)
            inspectState = null
        }
    }

    fun tick() {
        val locale = this.locale
        if (showHosts) {
            val hosts = plugin.lastHosts
            val possible = hosts.values.sumOf { it.possible }
            val marked = hosts.values.sumOf { it.marked }
            player.sendActionBar(plugin.i18n.safe(locale, "show_hosts") {
                raw("marked") { marked }
                raw("possible") { possible }
                raw("percent") { marked.toDouble() / possible }
                raw("mspt") { Bukkit.getAverageTickTime() }
                raw("tps") { Bukkit.getTPS()[0] }
            }.join(JoinConfiguration.noSeparators()))
        }

        inspectState?.let { inspect ->
            val origin = inspect.origin
            val ray = Ray(player.eyeLocation.vector(), player.location.direction.alexandria())
            plugin.effectors.player(player).apply {
                inspect.components.forEach { component ->
                    val position = origin + component.position
                    val bound = component.bound
                    //particleBound(ParticleEffect(key("minecraft:bubble_pop")), bound.translated(position), 0.1)
                    val feature = component.node.component.features[InspectFeature.ID]?.let {
                        it as InspectFeature.Profile
                    } ?: error("abc")
                    /*feature.slotOffsets.forEach { (_, offset) ->
                        showParticle(ParticleEffect(key("minecraft:bubble")), position + offset)
                    }*/
                    //todo val headPose = rotation.euler().degrees.y { -it }.z { -it } * fac
                    //component.render.rotate(headPose)
                }
            }

            data class ComponentBoundable(
                val component: InspectComponent,
                override val bound: Bound,
            ) : Boundable

            val raycast = FixedRaycast(inspect.components.map { ComponentBoundable(it, it.bound
                .translated(origin + it.position))
                // TODO rotation
            })
            when (val cast = raycast.cast(ray, 5.0)) {
                is Raycast.Result.Hit -> {
                    val component = cast.hit
                    if (inspect.selected != component.component) {
                        inspect.selected?.render?.deselect()
                        component.component.render.select()
                        inspect.selected = component.component
                    }
                }
                is Raycast.Result.Miss -> {
                    inspect.selected?.render?.deselect()
                    inspect.selected = null
                    player.clearTitle()
                }
            }

            val durability = (sin(System.currentTimeMillis() / 1000.0) * 0.5) + 0.5
            val barSize = 50
            val fillSize = (barSize * durability).toInt()

            inspect.selected?.let { selected ->
                /*player.showTitle(title(empty(), text()
                    .append(text("", null, TextDecoration.STRIKETHROUGH)
                        .append(text("\uf821".repeat(fillSize), DARK_GREEN))
                        .append(text("\uf821".repeat(barSize - fillSize), DARK_GRAY)))
                    .append(text(" %.0f%%".format(durability * 100), GREEN))
                    .build(), times(ZERO, ofMillis(250), ZERO)))

                 */
                player.showTitle(title(empty(), selected.node.component.localize(plugin.i18n).join(), times(ZERO, ofMillis(250), ZERO)))
            }
        }
    }
}
