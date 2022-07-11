package com.github.aecsocket.sokol.paper

import com.github.aecsocket.alexandria.core.LogLevel
import com.github.aecsocket.alexandria.core.effect.ParticleEffect
import com.github.aecsocket.alexandria.core.effect.SoundEffect
import com.github.aecsocket.alexandria.core.extension.*
import com.github.aecsocket.alexandria.core.keyed.by
import com.github.aecsocket.alexandria.core.physics.*
import com.github.aecsocket.alexandria.paper.datatype.QuaternionDataType
import com.github.aecsocket.alexandria.paper.effect.playGlobal
import com.github.aecsocket.alexandria.paper.extension.*
import com.github.aecsocket.sokol.core.errorMsg
import com.github.aecsocket.sokol.core.feature.RenderData
import com.github.aecsocket.sokol.core.feature.RenderFeature
import com.github.aecsocket.sokol.core.util.RenderMesh
import com.github.aecsocket.sokol.paper.extension.asStack
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.protocol.player.Equipment
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot
import com.github.retrooper.packetevents.util.Vector3d
import com.github.retrooper.packetevents.util.Vector3f
import com.github.retrooper.packetevents.wrapper.play.server.*
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import io.github.retrooper.packetevents.util.SpigotReflectionUtil
import net.kyori.adventure.text.Component.empty
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.World
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftAreaEffectCloud
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required
import java.util.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.properties.Delegates

private const val REL_MOVE_THRESH = 8.0

// 16 chars [a-z0-9_]
private const val TEAM_DEFAULT = "sokol_rd_def"
private const val TEAM_MARKED = "sokol_rd_mkd"
private const val TEAM_INVALID = "sokol_rd_inv"

// NodeRenders can be considered a "world" of `NodeRender` objects.
// Within one of these "worlds" the NodeRenders can interact with each other, e.g.
// make a new render out of a detached component
// *But* renders can't interact cross-world, unless some other piece of code lets it.
// Sokol stores one main instance of a NodeRenders "world".

// NodeRenders don't tick by themselves, but instead handle input from a single player at a time.
// The PlayerState passed in is the only data container it accesses
// (Sokol's main PlayerState stores a single NodeRenders.PlayerState for the main NodeRenders -
//  if a dependent wants to tick its own worlds, they must manage their own PlayerStates.)
interface NodeRenders<S : NodeRenders.PlayerState<P>, R : NodeRender<P>, P : NodePart> {
    interface PlayerState<P : NodePart> {
        val player: Player
        val selected: Selection<P>?
    }

    data class Selection<P : NodePart>(
        val part: P,
        var dragging: Boolean = false,
    )

    val entries: Collection<R>

    fun create(node: PaperDataNode, world: World, transform: Transform): R

    fun remove(render: R)

    fun select(state: S, part: P)

    fun deselect(state: S)

    fun dragging(state: S, dragging: Boolean)
}

interface NodeRender<P : NodePart> {
    val root: P
    var transform: Transform
}

data class NodeSlot<P : NodePart>(
    val transform: Transform,
    val part: P? = null
)

interface NodePart {
    val bodies: Collection<Body>
    val slots: Map<String, NodeSlot<*>>
}

class DefaultNodeRenders internal constructor(
    private val plugin: Sokol,
    var settings: Settings = Settings()
) : NodeRenders<
    DefaultNodeRenders.PlayerState,
    DefaultNodeRenders.Render,
    DefaultNodeRenders.Part
> {
    @ConfigSerializable
    data class Settings(
        val glowDefault: NamedTextColor = NamedTextColor.WHITE,
        val glowMarked: NamedTextColor = NamedTextColor.AQUA,
        val glowInvalid: NamedTextColor = NamedTextColor.RED,
        val rotation: Quaternion = Quaternion.Identity,
        val reach: Double = 3.0,
        val hold: Double = 2.0,
        val particleStep: Double = 0.2,
        val particleShape: ParticleEffect? = null,
        val particleAxes: AxisParticles? = null,
    ) {
        @ConfigSerializable
        data class AxisParticles(
            @Required val x: ParticleEffect,
            @Required val y: ParticleEffect,
            @Required val z: ParticleEffect,
        )
    }

    private val kRotation = plugin.key("rotation")
    private val kRenderNode = plugin.key("render_node")
    private val renders = HashMap<Int, Render>()
    override val entries get() = renders.values

    private fun sendTeams(player: Player, mode: WrapperPlayServerTeams.TeamMode) {
        fun send(name: String, color: NamedTextColor) {
            player.sendPacket(WrapperPlayServerTeams(
                name, mode,
                Optional.of(WrapperPlayServerTeams.ScoreBoardTeamInfo(
                    empty(), null, null,
                    WrapperPlayServerTeams.NameTagVisibility.NEVER,
                    WrapperPlayServerTeams.CollisionRule.NEVER,
                    color,
                    WrapperPlayServerTeams.OptionData.NONE
                ))
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
                sendTeams(player, WrapperPlayServerTeams.TeamMode.CREATE)
            }
        })
    }

    internal fun load() {
        settings = plugin.settings.nodeRenders
        bukkitPlayers.forEach { player ->
            sendTeams(player, WrapperPlayServerTeams.TeamMode.UPDATE)
        }
        renders.forEach { (_, render) ->
            // if the node is no longer valid after being backing copied
            // (its root component no longer exists)
            // it gets removed
            // maybe this behaviour should be changed?
            render.root.node.backedCopy(plugin)?.let { copy ->
                render.root.setupNode(copy, render.entity.trackedPlayers)
            } ?: run {
                plugin.log.line(LogLevel.Warning) { "Removed invalid render of ${render.root.node.component.id} at ${render.transform.tl}" }
                remove(render)
            }
        }
    }

    data class PlayerState(
        override val player: Player,
        override var selected: NodeRenders.Selection<Part>? = null,
        var showShapes: ShowShapes = ShowShapes.NONE,
        var lockSelection: Boolean = false,
    ) : NodeRenders.PlayerState<Part>

    enum class ShowShapes(val key: String) {
        NONE    ("none"),
        PART    ("part"),
        RENDER  ("render"),
        ALL     ("all"),
    }

    fun computePartTransform(player: Player, transform: Transform): Transform {
        val ray = player.eyeLocation.ray()
        val rotation = settings.rotation

        return plugin.raycast(player.world).castBlocks(ray, settings.reach) {
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
            ) + transform
        } ?: run {
            // · Z/forward/blue is pointing "inwards" to the player
            // · X/right/red points to the "right" of where the player's looking
            Transform(
                rot = quaternionLooking(-player.location.direction(), Vector3.Up) * rotation,
                tl = ray.point(settings.hold)
            )
        }
    }

    internal data class PartBody(
        override val shape: Shape,
        override val transform: Transform,
        val part: Part,
    ) : Body

    internal fun update() {
        val bodies = ArrayList<PartBody>()

        renders.forEach { (_, render) -> render.update(bodies) }

        plugin.playerState.forEach { (player, pState) ->
            val state = pState.renders
            if (!state.lockSelection) {
                val selected = state.selected
                if (selected == null || !selected.dragging) {
                    // TODO world raycast before this, check if blocks are in the way
                    raycastOf(bodies).cast(player.eyeLocation.ray(), settings.reach)?.let { col ->
                        select(state, col.hit.part)
                    } ?: run {
                        deselect(state)
                    }
                }
            }

            state.selected?.let { selected ->
                if (selected.dragging) {
                    val part = selected.part
                    if (part.isRoot) {
                        // move the entire render around
                        part.render.transform = computePartTransform(player, part.data.partTransform)
                    } else {
                        // move the single part
                        part.data.attach?.let { (axis, distance) ->
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
                            val pCo = part.baseTransform.tl
                            val pNo = (pCo - loc.vector()).normalized

                            iLinePlane(loc.vector(), loc.vector() + loc.direction(), pCo, pNo)?.let { intersect ->
                                // the distance our intersect is along this axis (- means it's backwards)
                                val dstMoved = part.baseTransform.invert(intersect).dot(axis)
                                part.dragTransform = Transform(
                                    tl = axis * clamp(dstMoved, 0.0, distance)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    internal fun handleSpawnEntity(player: Player, id: Int): Boolean {
        return renders[id]?.let {
            it.spawn(player)
            true
        } ?: run {
            SpigotReflectionUtil.getEntityById(id)?.let { entity ->
                try {
                    loadRender(entity)?.let {
                        it.spawn(player)
                        true
                    }
                } catch (ex: IllegalArgumentException) {
                    plugin.log.line(LogLevel.Warning, ex) { "Could not create render from entity due to state error - removing" }
                    entity.remove()
                    false
                } catch (ex: Exception) {
                    plugin.log.line(LogLevel.Warning, ex) { "Could not create render from entity" }
                    false
                }
            }
        } ?: false
    }

    internal fun handleDestroyEntities(player: Player, ids: IntArray) {
        ids.forEach { id ->
            renders[id]?.remove(setOf(player))
        }
    }

    internal fun handleDrag(state: PlayerState, value: Boolean?): Boolean {
        state.selected?.let { selected ->
            dragging(state, value ?: !selected.dragging)
            return true
        }
        return false
    }

    internal fun handleGrab(state: PlayerState): Boolean {
        state.selected?.let { selected ->
            val part = selected.part
            if (part.isRoot) {
                part.play(part.data.soundGrab)
                // TODO add to inv, and stuff
                remove(part.render)
            }
            return true
        }
        return false
    }

    fun create(entity: Entity, node: PaperDataNode, rotation: Quaternion): Render {
        fun PaperDataNode.part(isRoot: Boolean): Part {
            val render = node.component.features.by<RenderFeature.Profile<*>>(RenderFeature)
                ?: throw IllegalArgumentException(node.errorMsg("No feature '${RenderFeature.id}' to create render of"))
            return Part(this,
                isRoot,
                component.slots.map { (key) ->
                    key to NodeSlot(
                        render.slots[key]
                            ?: throw IllegalArgumentException(errorMsg("Feature '${RenderFeature.id}' does not have slot transform for '$key'")),
                        node(key)?.part(false)
                    )
                }.associate { it }
            ).also {
                it.setupNode(node)
            }
        }

        return Render(node.part(true), entity, rotation).also {
            // initialize transforms
            it.update(ArrayList())
            // initialize persistence
            it.save()
            fun Part.apply() {
                render = it
                slots.forEach { (_, slot) -> slot.part?.apply() }
            }
            it.root.apply()
            renders[it.id] = it
        }
    }

    override fun create(node: PaperDataNode, world: World, transform: Transform): Render {
        var render: Render? = null
        world.spawnEntity(
            transform.tl.location(world),
            EntityType.AREA_EFFECT_CLOUD,
            CreatureSpawnEvent.SpawnReason.CUSTOM
        ) { entity ->
            (entity as CraftAreaEffectCloud).handle.apply {
                // I know what I'm doing!
                tickCount = Int.MIN_VALUE
                duration = -1
                waitTime = Int.MIN_VALUE
            }
            render = create(entity, node, transform.rot)
        }
        return render ?: throw IllegalStateException("Render entity spawned but not returned (delayed entity spawn?)")
    }

    fun loadRender(entity: Entity): Render? {
        return plugin.persistence.tagOf(entity.persistentDataContainer, kRenderNode)?.let { tag ->
            val node = plugin.persistence.nodeOf(tag)
                ?: throw IllegalArgumentException("Entity has tag $kRenderNode but it was not a valid node")
            val rotation = entity.persistentDataContainer.getOrDefault(kRotation, QuaternionDataType, Quaternion.Identity)
            create(entity, node, rotation)
        }
    }

    fun remove(id: Int) {
        renders.remove(id)?.remove()
    }

    override fun remove(render: Render) {
        renders.remove(render.id)
        render.remove()
    }

    override fun select(state: PlayerState, part: Part) {
        deselect(state)
        state.selected = NodeRenders.Selection(part)
        part.select(state.player)
    }

    override fun deselect(state: PlayerState) {
        state.selected?.let {
            dragging(state, false)
            it.part.deselect(state.player)
            it.part.undoGlowState(state.player)
            state.selected = null
        }
    }

    override fun dragging(state: PlayerState, dragging: Boolean) {
        state.selected?.let { selected ->
            val part = selected.part
            if (selected.dragging == dragging) return
            selected.dragging = dragging
            part.glowState(state.player,
                if (dragging) PartGlowState.MARKED else PartGlowState.DEFAULT)

            part.play(if (dragging) part.data.soundDragStart else part.data.soundDragStop)

            if (!dragging) {
                part.dragTransform = Transform.Identity
                part.render.saveRotation()
            }
        }
    }

    // A single NodeRender is attached to a NodeRenders (its "world" - read above)
    // It is also backed by a physical Bukkit entity - one which exists in the world, is serialized etc.
    //  · This entity is never actually shown to clients
    //  · The armor stands spawned by the meshes of this render are all virtual
    inner class Render(
        override val root: Part,
        val entity: Entity,
        var rotation: Quaternion,
    ) : NodeRender<Part> {
        val id = entity.entityId
        override var transform: Transform
            get() = entity.transform.copy(rot = rotation)
            set(value) {
                rotation = value.rot
                entity.transform = value
            }

        internal var lastTracked: Set<Player> = emptySet()

        internal fun update(bodies: MutableList<PartBody>) {
            lastTracked = entity.trackedPlayers
            fun Part.apply(tf: Transform) {
                baseTransform = tf + data.partTransform
                transform = tf + dragTransform

                this.bodies.forEach { body ->
                    val bodyTf = transform + body.transform
                    bodies.add(PartBody(body.shape, bodyTf, this))
                    lastTracked.forEach { player ->
                        val pState = plugin.playerState(player)
                        val state = pState.renders
                        if (when (state.showShapes) {
                                ShowShapes.NONE -> false
                                ShowShapes.PART -> state.selected?.part == this
                                ShowShapes.RENDER -> state.selected?.part?.render == this@Render
                                ShowShapes.ALL -> true
                            }
                        ) {
                            settings.particleShape?.let {
                                pState.effector.showShape(it, body.shape, bodyTf, settings.particleStep)
                            }
                        }
                    }
                }

                meshes.forEach { mesh ->
                    val meshTf = mesh.computeTransform(transform)
                    mesh.sendTransform(lastTracked, meshTf)
                }
                slots.forEach { (_, slot) ->
                    slot.part?.let {
                        it.apply(transform + slot.transform + it.data.attachedTransform)
                    }
                }
            }
            root.apply(transform)

            val axesFrom = transform.tl
            val toX = axesFrom + (transform.rot * Vector3.Right)
            val toY = axesFrom + (transform.rot * Vector3.Up)
            val toZ = axesFrom + (transform.rot * Vector3.Forward)
            val step = settings.particleStep

            lastTracked.forEach { player ->
                val pState = plugin.playerState(player)
                val state = pState.renders
                if (when (state.showShapes) {
                    ShowShapes.NONE -> false
                    ShowShapes.PART, ShowShapes.RENDER -> state.selected?.part?.render == this
                    ShowShapes.ALL -> true
                }) {
                    settings.particleAxes?.let {
                        pState.effector.showLine(it.x, axesFrom, toX, step)
                        pState.effector.showLine(it.y, axesFrom, toY, step)
                        pState.effector.showLine(it.z, axesFrom, toZ, step)
                    }
                }
            }
        }

        fun saveRotation() {
            entity.persistentDataContainer.set(kRotation, QuaternionDataType, rotation)
        }

        fun saveNode() {
            plugin.persistence.tagTo(
                plugin.persistence.newTag().apply { plugin.persistence.nodeInto(root.node, this) },
                entity.persistentDataContainer,
                kRenderNode
            )
        }

        fun save() {
            saveRotation()
            saveNode()
        }

        fun spawn(player: Player) {
            fun Part.apply() {
                meshes.forEach { mesh ->
                    mesh.sendSpawn(player, transform)
                }
                slots.forEach { (_, slot) -> slot.part?.apply() }
            }
            root.apply()
        }

        fun remove(players: Iterable<Player>) {
            val ids = ArrayList<Int>()

            fun Part.apply() {
                meshes.forEach { ids.add(it.entityId) }
                slots.forEach { (_, slot) -> slot.part?.apply() }
            }
            root.apply()

            val idsArr = ids.toIntArray()
            players.forEach { player ->
                player.sendPacket(WrapperPlayServerDestroyEntities(*idsArr))
            }
        }

        fun remove() {
            // if the entity is already removed, `trackedPlayers` will give an empty set
            remove(lastTracked)
        }
    }

    enum class PartGlowState {
        DEFAULT,
        MARKED,
        INVALID,
    }

    inner class Part(
        node: PaperDataNode,
        val isRoot: Boolean,
        override var slots: Map<String, NodeSlot<Part>>,
    ) : NodePart {
        var node: PaperDataNode = node
            private set

        internal fun setupNode(node: PaperDataNode, players: Iterable<Player>? = null) {
            val render = node.component.features.by<RenderFeature.Profile<*>>(RenderFeature)
                ?: throw IllegalArgumentException(node.errorMsg("No feature '${RenderFeature.id}' to set part parameters from"))

            // remove old meshes if we have players to remove them for
            val ids = meshes.map { it.entityId }.toIntArray()
            players?.forEach {
                it.sendPacket(WrapperPlayServerDestroyEntities(*ids))
            }

            bodies = render.bodies
            meshes = render.meshes.map {
                Mesh(
                    node, it, bukkitNextEntityId, UUID.randomUUID(),
                    when (it) {
                        is RenderMesh.Static -> it.item.asStack()
                        is RenderMesh.Dynamic -> {
                            // we don't want any children to be rendered on this node
                            // e.g. if a root node changes model when it is complete
                            plugin.persistence.nodeToStack(node.copy().apply { removeChildren() })
                        }
                    }
                ).also { mesh ->
                    // if `players` is null, then we're probably setting up the node for the first time
                    // at that time, `transform` is not initialized since update() hasn't been called yet
                    players?.let { players ->
                        // spawn new meshes
                        val meshTf = mesh.computeTransform(transform)
                        players.forEach { player ->
                            mesh.sendSpawn(player, meshTf)
                        }
                    }
                }
            }
            data = render.data

            slots = node.component.slots.map { (key) ->
                key to NodeSlot(
                    render.slots[key]
                        ?: throw IllegalArgumentException(node.errorMsg("Feature '${RenderFeature.id}' does not have slot transform for '$key'")),
                    slots[key]?.part?.let { existing ->
                        // if the node we just set to, has a child for this key,
                        // we assign the existing part this new node
                        // else we just remove the part
                        node.node(key)?.let {
                            existing.setupNode(it, players)
                            existing
                        }
                    }
                )
            }.associate { it }
            this.node = node
        }

        override lateinit var bodies: Collection<Body>
        var meshes: Collection<Mesh> = emptySet()
        lateinit var data: RenderData
        var dragTransform = Transform.Identity

        lateinit var render: Render
            internal set
        lateinit var baseTransform: Transform
            internal set
        lateinit var transform: Transform
            internal set

        private var lastGlowState: PartGlowState? = null

        fun play(sounds: Iterable<SoundEffect>) {
            sounds.playGlobal(plugin.effectors, render.entity.world, transform.tl)
        }

        fun select(player: Player) {
            meshes.forEach { it.sendGlow(player, true) }
        }

        fun deselect(player: Player) {
            meshes.forEach { it.sendGlow(player, false) }
        }

        fun undoGlowState(player: Player) {
            lastGlowState?.let { state ->
                meshes.forEach { it.sendGlowState(player, state, false) }
            }
            lastGlowState = null
        }

        fun glowState(player: Player, state: PartGlowState) {
            undoGlowState(player)
            meshes.forEach { it.sendGlowState(player, state, true) }
            lastGlowState = state
        }
    }

    data class Mesh(
        val node: PaperDataNode,
        val backing: RenderMesh,
        val entityId: Int,
        val uuid: UUID,
        val item: ItemStack,
    ) {
        var lastPos: Vector3? = null

        fun computeTransform(base: Transform) = base + backing.transform

        fun sendSpawn(player: Player, transform: Transform) {
            val pos = transform.tl
            player.sendPacket(WrapperPlayServerSpawnEntity(
                entityId, Optional.of(uuid), EntityTypes.ARMOR_STAND,
                Vector3d(pos.x, pos.y - 1.45, pos.z), 0f, 0f, 0f, 0, Optional.empty()
            ))

            player.sendPacket(WrapperPlayServerEntityMetadata(entityId, listOf(
                EntityData(0, EntityDataTypes.BYTE,
                    (0x20).toByte()), // invisible
                EntityData(15, EntityDataTypes.BYTE,
                    (0x10).toByte()), // marker
            )))

            player.sendPacket(WrapperPlayServerEntityEquipment(entityId, listOf(
                Equipment(EquipmentSlot.HELMET, SpigotConversionUtil.fromBukkitItemStack(item))
            )))

            sendPose(player, computePose(transform.rot))
        }

        fun computePose(rotation: Quaternion): Vector3f {
            val (rx, ry, rz) = rotation.euler(EulerOrder.ZYX).x { -it }.degrees
            return Vector3f(rx.toFloat(), ry.toFloat(), rz.toFloat())
        }

        fun sendPose(player: Player, pose: Vector3f) {
            player.sendPacket(WrapperPlayServerEntityMetadata(entityId, listOf(
                EntityData(16, EntityDataTypes.ROTATION, pose)
            )))
        }

        fun sendTransform(players: Iterable<Player>, transform: Transform) {
            val pos = transform.tl
            val pose = computePose(transform.rot)

            lastPos?.let { lastPos ->
                // if lastPos hasn't been set yet, we've probably just spawned the entity in
                // so no need to set its position
                players.forEach { player ->
                    val (dx, dy, dz) = pos - lastPos
                    player.sendPacket(
                        if (abs(dx) > REL_MOVE_THRESH || abs(dy) > REL_MOVE_THRESH || abs(dz) > REL_MOVE_THRESH) {
                            WrapperPlayServerEntityRelativeMove(entityId,
                                dx, dy, dz, true
                            )
                        } else {
                            WrapperPlayServerEntityTeleport(
                                entityId,
                                Vector3d(pos.x, pos.y - 1.45, pos.z), 0f, 0f, true
                            )
                        }
                    )
                }
            }
            lastPos = pos

            players.forEach { player -> sendPose(player, pose) }
        }

        fun sendGlow(player: Player, state: Boolean) {
            player.sendPacket(WrapperPlayServerEntityMetadata(entityId, listOf(
                EntityData(0, EntityDataTypes.BYTE,
                    (0x20 or if (state) 0x40 else 0).toByte()) // invisible + glowing?
            )))
        }

        fun sendGlowState(player: Player, state: PartGlowState, add: Boolean) {
            player.sendPacket(WrapperPlayServerTeams(when (state) {
                PartGlowState.DEFAULT -> TEAM_DEFAULT
                PartGlowState.MARKED -> TEAM_MARKED
                PartGlowState.INVALID -> TEAM_INVALID
            }, if (add) WrapperPlayServerTeams.TeamMode.ADD_ENTITIES else WrapperPlayServerTeams.TeamMode.REMOVE_ENTITIES,
            Optional.empty(), uuid.toString()))
        }
    }
}
