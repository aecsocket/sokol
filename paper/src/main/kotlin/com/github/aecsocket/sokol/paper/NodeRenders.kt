package com.github.aecsocket.sokol.paper

import com.github.aecsocket.alexandria.core.effect.ParticleEffect
import com.github.aecsocket.alexandria.core.extension.*
import com.github.aecsocket.alexandria.core.keyed.by
import com.github.aecsocket.alexandria.core.physics.*
import com.github.aecsocket.alexandria.paper.extension.*
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
import com.github.retrooper.packetevents.util.Vector3f
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRelativeMove
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.ScoreBoardTeamInfo
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.TeamMode
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import net.kyori.adventure.text.Component.empty
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.PI
import kotlin.math.abs

private const val REL_MOVE_THRESH = 8.0

// 16 chars [a-z0-9_]
private const val TEAM_DEFAULT = "sokol_rd_def"
private const val TEAM_MARKED = "sokol_rd_mkd"
private const val TEAM_INVALID = "sokol_rd_inv"

data class SpawnedMesh(
    val backing: RenderMesh,
    val entityId: Int,
    val uuid: UUID,
    val item: ItemStack,
    var lastPos: Vector3? = null,
) {
    fun transform(baseTf: Transform) = baseTf + backing.transform

    fun sendGlowing(player: Player, value: Boolean) {
        player.sendPacket(WrapperPlayServerEntityMetadata(entityId, listOf(
            EntityData(0, EntityDataTypes.BYTE,
                (0x20 or if (value) 0x40 else 0).toByte()), // invisible + glowing?
        )))
    }

    fun sendSelectionState(player: Player, to: NodePart.SelectionState, from: NodePart.SelectionState?) {
        if (to == from) return

        fun send(state: NodePart.SelectionState, add: Boolean) {
            player.sendPacket(WrapperPlayServerTeams(
                when (state) {
                    NodePart.SelectionState.DEFAULT -> TEAM_DEFAULT
                    NodePart.SelectionState.MARKED -> TEAM_MARKED
                    NodePart.SelectionState.INVALID -> TEAM_INVALID
                },
                if (add) TeamMode.ADD_ENTITIES else TeamMode.REMOVE_ENTITIES,
                Optional.empty(),
                uuid.toString(),
            ))
        }

        from?.let { send(it, false) }
        send(to, true)
    }
}

private fun spawnedMeshOf(mesh: RenderMesh, node: PaperDataNode): SpawnedMesh {
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

data class NodeSlot(
    val transform: Transform,
    var part: NodePart? = null,
)

data class NodePart(
    val node: PaperDataNode,
    val bodies: Collection<Body>,
    val meshes: Collection<SpawnedMesh>,
    val attachedTf: Transform,
    val snapTf: Transform,
    val attachAxis: Vector3?,
    val attachDistance: Double,
    val slots: Map<String, NodeSlot>,
    var dragTf: Transform = Transform.Identity,
) {
    enum class SelectionState {
        DEFAULT,
        MARKED,
        INVALID,
    }
}

private fun partOf(node: PaperDataNode): NodePart {
    val render = node.component.features.by<PaperRender.Profile>(RenderFeature)
        ?: throw IllegalStateException(node.errorMsg("No feature '${RenderFeature.id}'"))
    return NodePart(
        node,
        render.bodies,
        render.meshes.map { spawnedMeshOf(it, node) },
        render.attachedTransform,
        render.snapTransform,
        if (render.attachAxis.sqrLength > 0.0) render.attachAxis.normalized else null,
        render.attachDistance,
        node.component.slots.map { (key, _) ->
            key to NodeSlot(
                render.slots[key]
                    ?: throw IllegalStateException(node.errorMsg("No slot transform for slot '$key'")),
                node.node(key)?.let { partOf(it) }
            )
        }.associate { it }
    )
}

class NodeRender(
    private val manager: NodeRenders,
    val backing: Entity,
    var rotation: Quaternion,
    val root: NodePart,
) {
    val id = backing.entityId

    data class Tracking(
        val active: Boolean = false,
    )

    data class Selected(
        val render: NodeRender,
        val part: NodePart,
        var dragging: Boolean = false,
        var lastState: NodePart.SelectionState? = null,
    )

    enum class ShowShape(val key: String) {
        NONE    ("none"),
        PART    ("part"),
        RENDER  ("render"),
        ALL     ("all"),
    }

    var transform: Transform
        get() = backing.transform.copy(rot = rotation)
        set(value) {
            backing.transform = value
            rotation = value.rot
        }

    fun write() {
        manager.plugin.persistence.setRender(root.node, rotation, backing.persistentDataContainer)
    }

    private fun walk(action: (NodePart, Transform, Transform) -> Unit) {
        fun apply(part: NodePart, baseTf: Transform) {
            val tf = baseTf + part.dragTf
            action(part, tf, baseTf)
            part.slots.forEach { (_, slot) -> slot.part?.let { apply(it, tf + slot.transform + it.attachedTf) } }
        }

        apply(root, transform)
    }

    fun SpawnedMesh.sendSpawn(player: Player, tf: Transform) {
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

    fun SpawnedMesh.sendTransform(player: Player, tf: Transform) {
        val dst = tf.tl
        lastPos?.let { lastPos ->
            val (dx, dy, dz) = dst - lastPos
            if (abs(dx) > REL_MOVE_THRESH || abs(dy) > REL_MOVE_THRESH || abs(dz) > REL_MOVE_THRESH) {
                // send teleport packet
                player.sendPacket(WrapperPlayServerEntityTeleport(entityId,
                    Vector3d(dst.x, dst.y, dst.z), 0f, 0f, true
                ))
            } else {
                // send relative move
                player.sendPacket(WrapperPlayServerEntityRelativeMove(entityId,
                    dx, dy, dz, true
                ))
            }

            // Minecraft uses ZYX
            val (rx, ry, rz) = tf.rot.euler(EulerOrder.ZYX).degrees.x { -it }
            player.sendPacket(WrapperPlayServerEntityMetadata(entityId, listOf(
                EntityData(16, EntityDataTypes.ROTATION, // head pose
                    Vector3f(rx.toFloat(), ry.toFloat(), rz.toFloat()))
            )))
        }
        lastPos = dst
    }

    fun spawn(players: Iterable<Player>) {
        walk { part, tf, _ ->
            part.meshes.forEach { mesh ->
                val meshTf = mesh.transform(tf)
                players.forEach {
                    mesh.sendSpawn(it, meshTf)
                    mesh.sendTransform(it, meshTf)
                }
            }
        }
    }

    fun remove(players: Iterable<Player>) {
        val ids = ArrayList<Int>()
        walk { part, _, _ ->
            part.meshes.forEach { mesh ->
                ids.add(mesh.entityId)
            }
        }
        val arrIds = ids.toIntArray()
        players.forEach { it.sendPacket(WrapperPlayServerDestroyEntities(*arrIds)) }
    }

    internal fun tick(players: Iterable<PlayerData>, bodies: MutableList<PartBody>) {
        walk { part, tf, baseTf ->
            part.bodies.forEach { body ->
                val bodyTf = tf + body.transform
                bodies.add(PartBody(this, part, body.shape, bodyTf))
                players.forEach { data ->
                    if (when (data.rdShowShapes) {
                        ShowShape.NONE -> false
                        ShowShape.PART -> data.rdSelected?.part === part
                        ShowShape.RENDER -> data.rdSelected?.render === this
                        ShowShape.ALL -> true
                    }) {
                        manager.settings.shapeParticle?.let {
                            data.effector.showShape(it, body.shape, bodyTf, manager.settings.shapeStep)
                        }
                    }
                }
            }

            part.meshes.forEach { mesh ->
                val meshTf = tf + mesh.backing.transform
                players.forEach { mesh.sendTransform(it.player, meshTf) }
            }

            val isRoot = root === part
            players.forEach { data ->
                val player = data.player
                data.rdSelected?.let { selected ->
                    if (selected.render != this || selected.part != part || !selected.dragging) return@let

                    if (isRoot) {
                        // move the entire render around
                        transform = manager.createPartTransform(player, part.snapTf)
                    } else {
                        // move the single part
                        part.attachAxis?.let { axis ->
                            fun iLinePlane(p0: Vector3, p1: Vector3, pCo: Vector3, pNo: Vector3): Vector3? {
                                // https://stackoverflow.com/questions/5666222/3d-line-plane-intersection
                                val u = p1 - p0
                                val dot = pNo.dot(u)

                                if (abs(dot) > EPSILON) {
                                    val w = p0 - pCo
                                    val fac = -pNo.dot(w) / dot
                                    return p0 + (u * fac)
                                }

                                // line parallel to plane
                                return null
                            }

                            //  · to determine how far along part.attachAxis we transform our part,
                            //    we find the intersection of a ray and a line:
                            //    · p0 is the player's eye
                            //    · p1 is the player's eye + player dir (basically another point along their dir line)
                            //    · pCo and pNo define a plane along the part
                            //      · pCo is the part's transform
                            //      · pNo is the normal that makes the plane "point" towards the player
                            //  · get the intersection of these, and you can get the transform along the drag line
                            val loc = player.eyeLocation
                            val pCo = baseTf.apply()
                            val pNo = (pCo - loc.vector()).normalized

                            iLinePlane(loc.vector(), loc.vector() + loc.direction(), pCo, pNo)?.let { intersect ->
                                // the distance our intersect is along this axis (- means it's backwards)
                                val dstMoved = baseTf.invert(intersect).dot(axis)
                                part.dragTf = Transform(
                                    tl = axis * clamp(dstMoved, 0.0, part.attachDistance)
                                )
                            }
                        }
                    }
                }
            }
        }

        // `from` = render origin
        // `to[X|Y|Z]` = origin + that axis in local space, to world space (think of the arrows when moving an object in Unity)
        val tf = transform
        val from = tf.apply()
        val toX = from + (tf.rot * Vector3.Right)
        val toY = from + (tf.rot * Vector3.Up)
        val toZ = from + (tf.rot * Vector3.Forward)
        val step = manager.settings.shapeStep
        players.forEach { data ->
            if (when (data.rdShowShapes) {
                ShowShape.NONE -> false
                else -> true
            }) {
                manager.settings.xParticle?.let { data.effector.showLine(it, from, toX, step) }
                manager.settings.yParticle?.let { data.effector.showLine(it, from, toY, step) }
                manager.settings.zParticle?.let { data.effector.showLine(it, from, toZ, step) }
            }
        }
    }
}

internal data class PartBody(
    val render: NodeRender,
    val part: NodePart,
    override val shape: Shape,
    override val transform: Transform,
) : Body

class NodeRenders internal constructor(
    internal val plugin: SokolPlugin,
    var settings: Settings = Settings(),
) {
    @ConfigSerializable
    data class Settings(
        val glowDefault: NamedTextColor = NamedTextColor.WHITE,
        val glowMarked: NamedTextColor = NamedTextColor.AQUA,
        val glowInvalid: NamedTextColor = NamedTextColor.RED,
        val holdDistance: Double = 3.0,
        val reachDistance: Double = 4.0,
        val rotation: Quaternion = Quaternion.Identity,

        val shapeParticle: ParticleEffect? = null,
        val shapeStep: Double = 0.2,
        val xParticle: ParticleEffect? = null,
        val yParticle: ParticleEffect? = null,
        val zParticle: ParticleEffect? = null,
    )

    private val _entries = HashMap<Int, NodeRender>()
    val entries: Map<Int, NodeRender> get() = _entries

    private fun sendTeams(player: Player, mode: TeamMode) {
        fun send(name: String, color: NamedTextColor) {
            player.sendPacket(WrapperPlayServerTeams(
                name, mode,
                Optional.of(ScoreBoardTeamInfo(empty(), null, null,
                    WrapperPlayServerTeams.NameTagVisibility.NEVER,
                    WrapperPlayServerTeams.CollisionRule.NEVER,
                    color,
                    WrapperPlayServerTeams.OptionData.NONE)
                )
            ))
        }

        send(TEAM_DEFAULT, settings.glowDefault)
        send(TEAM_MARKED, settings.glowMarked)
        send(TEAM_INVALID, settings.glowInvalid)
    }

    internal fun init() {
        plugin.registerEvents(object : Listener {
            @EventHandler
            fun PlayerJoinEvent.on() {
                sendTeams(player, TeamMode.CREATE)
            }
        })
    }

    internal fun loadSettings() {
        settings = plugin.settings.nodeRenders
        bukkitPlayers.forEach { player ->
            sendTeams(player, TeamMode.UPDATE)
        }
    }

    operator fun get(id: Int) = _entries[id]

    fun create(backing: Entity, node: PaperDataNode, rotation: Quaternion): NodeRender {
        return NodeRender(this, backing, rotation, partOf(node)).also {
            _entries[it.id] = it
        }
    }

    fun remove(id: Int) {
        _entries.remove(id)?.let { render ->
            render.remove(render.trackedBy.map { it.player })
        }
    }

    private val NodeRender.trackedBy get() = plugin.playerData
        .filter { (_, data) -> data.rdTracking.containsKey(backing.entityId) }
        .values

    fun startTracking(player: Player, render: NodeRender) {
        plugin.playerData(player).rdTracking[render.id] = NodeRender.Tracking()
        render.spawn(setOf(player))
    }

    fun stopTracking(player: Player, render: NodeRender) {
        plugin.playerData(player).rdTracking.remove(render.id)
        render.remove(setOf(player))
    }

    private fun PlayerData.rdSelected(value: NodeRender.Selected?) {
        rdSelected?.let { old ->
            old.part.dragTf = Transform.Identity
            old.part.meshes.forEach {
                it.sendGlowing(player, false)
                it.sendSelectionState(player, NodePart.SelectionState.DEFAULT, old.lastState)
            }
        }
        value?.let { new ->
            new.part.meshes.forEach {
                it.sendGlowing(player, true)
                it.sendSelectionState(player, NodePart.SelectionState.DEFAULT, new.lastState)
            }
        }
        rdSelected = value
    }

    fun interact(data: PlayerData, state: Boolean?): Boolean {
        data.rdSelected?.let { selected -> selected.apply {
            dragging = state ?: !dragging
            part.meshes.forEach {
                it.sendSelectionState(data.player,
                    if (dragging) NodePart.SelectionState.MARKED else NodePart.SelectionState.DEFAULT,
                    lastState,
                )
            }
            part.dragTf = Transform.Identity
            if (!dragging) {
                // write into PDC once done dragging
                render.write()
            }
            return true
        } }
        return false
    }

    fun createPartTransform(player: Player, snapTf: Transform): Transform {
        val ray = player.eyeLocation.ray()
        val rotation = settings.rotation
        val reach = settings.reachDistance

        return plugin.raycast(player.world).castBlocks(ray, reach) {
            it.fluid == null
        }?.let { col ->
            val rot = if (abs(col.normal.y) < EPSILON) quaternionLooking(col.normal, Vector3.Up)
            else {
                val yaw = player.location.yaw.radians.toDouble()
                // `up` and `normal` are collinear - player's looking at a vertical
                quaternionFromTo(Vector3.Forward, col.normal) *
                    // TODO this is a stupid hack
                    Euler3(z = if (col.normal.y > 0.0) -yaw + PI else yaw + PI)
                        .quaternion(EulerOrder.XYZ)
            }

            Transform(
                rot = rot * rotation,
                tl = col.posIn
            ) + snapTf
        } ?: run {
            // · Z/forward/blue is pointing "inwards" to the player
            // · X/right/red points to the "right" of where the player's looking
            Transform(
                rot = quaternionLooking(-player.location.direction(), Vector3.Up) * rotation,
                tl = ray.point(settings.holdDistance)
            )
        }
    }

    internal fun tick() {
        val bodies = ArrayList<PartBody>()

        _entries.forEach { (_, render) ->
            render.tick(render.trackedBy, bodies)
        }

        // here we only change selection status
        // dragging and transforms happen in the part tick
        plugin.playerData.forEach { (player, data) ->
            val ray = player.eyeLocation.ray()
            val reach = settings.reachDistance

            if (data.rdSelected?.dragging != true) {
                raycastOf(bodies).cast(ray, reach)?.let { col ->
                    val (render, part) = col.hit
                    data.rdSelected(NodeRender.Selected(render, part))
                } ?: run {
                    data.rdSelected(null)
                }
            }
        }
    }
}
