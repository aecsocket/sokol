package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.core.Input
import com.gitlab.aecsocket.alexandria.core.LogLevel
import com.gitlab.aecsocket.alexandria.core.effect.Effector
import com.gitlab.aecsocket.alexandria.core.effect.ParticleEffect
import com.gitlab.aecsocket.alexandria.core.effect.SoundEffect
import com.gitlab.aecsocket.alexandria.core.extension.*
import com.gitlab.aecsocket.alexandria.core.keyed.by
import com.gitlab.aecsocket.alexandria.core.physics.*
import com.gitlab.aecsocket.alexandria.paper.datatype.QuaternionDataType
import com.gitlab.aecsocket.alexandria.paper.effect.playGlobal
import com.gitlab.aecsocket.alexandria.paper.extension.*
import com.gitlab.aecsocket.alexandria.paper.input.*
import com.gitlab.aecsocket.sokol.core.errorMsg
import com.gitlab.aecsocket.sokol.core.feature.RenderPartData
import com.gitlab.aecsocket.sokol.core.feature.RenderFeature
import com.gitlab.aecsocket.sokol.core.feature.RenderJointData
import com.gitlab.aecsocket.sokol.core.util.RenderMeshData
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.protocol.player.Equipment
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot
import com.github.retrooper.packetevents.util.Vector3d
import com.github.retrooper.packetevents.util.Vector3f
import com.github.retrooper.packetevents.wrapper.play.server.*
import com.gitlab.aecsocket.sokol.core.feature.HostCreationException
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import io.github.retrooper.packetevents.util.SpigotReflectionUtil
import net.kyori.adventure.text.Component.empty
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.World
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftAreaEffectCloud
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.abs

private const val ROTATE_TOGGLE = "rotate_toggle"
private const val MOVE_TOGGLE = "move_toggle"
private const val ROTATE_START = "rotate_start"
private const val MOVE_START = "move_start"
private const val DRAG_STOP = "drag_stop"
private const val GRAB = "grab"

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
    @ConfigSerializable
    data class Options(
        val selectable: Boolean = true,
        val movable: Boolean = true,
        val rotatable: Boolean = true,
        val grabbable: Boolean = true,
        val modifiable: Boolean = true,
        val unrestricted: Boolean = false,
    ) {
        val value get() =
            (if (selectable) 0b1 else 0) or
            (if (movable) 0b10 else 0) or
            (if (rotatable) 0b100 else 0) or
            (if (grabbable) 0b1000 else 0) or
            (if (modifiable) 0b10000 else 0) or
            (if (unrestricted) 0b100000 else 0)

        companion object {
            fun of(value: Int) = Options(
                (value and 0b1) != 0,
                (value and 0b10) != 0,
                (value and 0b100) != 0,
                (value and 0b1000) != 0,
                (value and 0b10000) != 0,
                (value and 0b100000) != 0,
            )
        }
    }

    interface PlayerState<P : NodePart> {
        val player: Player
        val selected: Selection<P>?
    }

    data class Selection<P : NodePart>(
        val part: P,
        var dragging: DragState = DragState.NONE,
        var rotationPlaneNormal: Vector3? = null,
        var snapTo: SnapTarget<P>? = null,
    )

    enum class DragState(val active: Boolean) {
        NONE        (false),
        INVALID     (true),
        MOVING      (true),
        ROTATING    (true),
    }

    data class SnapTarget<P : NodePart>(
        val slot: PartSlot<P>,
        val compatible: Boolean,
    )

    val entries: Collection<R>

    fun create(node: PaperDataNode, world: World, transform: Transform, options: Options? = null): R

    fun select(state: S, part: P)

    fun deselect(state: S)

    fun dragging(state: S, dragging: DragState)
}

interface NodeRender<P : NodePart> {
    val root: P
    val world: World
    var transform: Transform

    fun remove()
}

data class PartSlot<P : NodePart>(
    val parent: P,
    val key: String,
    val data: RenderJointData,
    var part: P? = null
)

interface NodePart {
    var node: PaperDataNode
    val bodies: Collection<Body>
    val slots: Map<String, PartSlot<*>>
}

class DefaultNodeRenders internal constructor(
    private val plugin: Sokol,
    var settings: Settings = Settings(),
) : NodeRenders<
    DefaultNodeRenders.PlayerState,
    DefaultNodeRenders.Render,
    DefaultNodeRenders.Part
> {
    @ConfigSerializable
    data class Settings(
        val defaultOptions: NodeRenders.Options = NodeRenders.Options(),
        val glowDefault: NamedTextColor = NamedTextColor.WHITE,
        val glowMarked: NamedTextColor = NamedTextColor.AQUA,
        val glowValid: NamedTextColor = NamedTextColor.GREEN,
        val glowInvalid: NamedTextColor = NamedTextColor.RED,
        val rotation: Quaternion = Quaternion.Identity,
        val reach: Double = 3.0,
        val hold: Double = 2.0,
        val inputs: InputMapper = InputMapper(mapOf(
            INPUT_MOUSE to listOf(
                InputPredicate(setOf(Input.MouseButton.RIGHT.key, Input.MouseState.UNDEFINED.key), MOVE_TOGGLE),
                InputPredicate(setOf(Input.MouseButton.RIGHT.key, Input.MouseState.UNDEFINED.key, PLAYER_SNEAKING), ROTATE_TOGGLE),
                InputPredicate(setOf(Input.MouseButton.RIGHT.key, Input.MouseState.DOWN.key), MOVE_START),
                InputPredicate(setOf(Input.MouseButton.RIGHT.key, Input.MouseState.DOWN.key, PLAYER_SNEAKING), ROTATE_START),
                InputPredicate(setOf(Input.MouseButton.RIGHT.key, Input.MouseState.UP.key), DRAG_STOP),

                InputPredicate(setOf(Input.MouseButton.LEFT.key, Input.MouseState.UNDEFINED.key), GRAB),
                InputPredicate(setOf(Input.MouseButton.LEFT.key, Input.MouseState.DOWN.key), GRAB),
            )
        )),

        val particleStep: Double = 0.2,
        val particleBody: ParticleEffect? = null,
        val particleSlot: ParticleEffect? = null,
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
    private val kOptions = plugin.key("options")
    private val renders = ConcurrentHashMap<Int, Render>()
    override val entries get() = renders.values
    private val hideIds = HashSet<Int>()

    private fun Effector.showShape(
        effect: ParticleEffect,
        shape: Shape,
        transform: Transform,
    ) = showShape(effect, shape, transform, settings.particleStep)

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
        send(TEAM_VALID, settings.glowValid)
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
            // if the node is no longer valid after being backing copied (its root component no longer exists)
            // we log a warning, but *don't* remove it (priority is to avoid any data loss)
            render.root.node.backedCopy(plugin)?.let { copy ->
                render.root.node = copy
            } ?: run {
                plugin.log.line(LogLevel.Warning) { "Invalid render of ${render.root.node.component.id} at ${render.transform.tl}" }
            }
        }
    }

    data class PlayerState(
        override val player: Player,
        val effector: Effector,
        override var selected: NodeRenders.Selection<Part>? = null,
        var showShapes: ShowShapes = ShowShapes.NONE,
        var bypassOptions: Boolean = false,
        var lockSelection: Boolean = false,
        var lastNormal: Vector3? = null,
    ) : NodeRenders.PlayerState<Part>

    enum class ShowShapes(val active: Boolean, val key: String) {
        NONE    (false, "none"),
        PART    (true, "part"),
        RENDER  (true, "render"),
        ALL     (true, "all"),
    }

    fun computePartTransform(state: PlayerState, surfaceOffset: Double, lastTransform: Transform? = null): Transform {
        val player = state.player
        val ray = player.eyeLocation.ray()
        val rotation = settings.rotation

        return plugin.raycast(player.world).castBlocks(ray, settings.reach) {
            it.fluid == null
        }?.let { col ->
            val normal = col.normal
            // determine the rotation:
            // · if the normal is not vertical (on a wall):
            //   · if it's the same as our player's last normal, keep the old rotation
            //   · if it's a different normal (different block face?), compute a new rot
            // · else it's vertical, pointing away from the player
            val rot = if (abs(normal.y) < EPSILON) {
                if (lastTransform != null && (state.lastNormal == null || normal == state.lastNormal)) lastTransform.rot
                else quaternionLooking(normal, Vector3.Up) * rotation
            } else {
                val yaw = player.location.yaw.radians.toDouble()
                // `up` and `normal` are collinear - player's looking at a vertical
                quaternionFromTo(Vector3.Forward, normal) *
                    // TODO this is a stupid hack
                    Euler3(z = if (normal.y > 0.0) -yaw + PI else yaw + PI).quaternion(EulerOrder.XYZ) *
                    rotation
            }
            state.lastNormal = normal
            Transform(
                rot = rot,
                tl = col.posIn + normal * surfaceOffset
            )
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

    internal data class SlotBody(
        override val shape: Shape,
        override val transform: Transform,
        val slot: PartSlot<Part>,
    ) : Body

    private val PartSlot<Part>.modifiable get(): Boolean {
        val opts = parent.render.options()
        return opts.unrestricted || data.modifiable
    }

    fun update() {
        hideIds.clear()
        val partBodies = HashMap<World, MutableSet<PartBody>>()
        val slotBodies = HashMap<World, MutableSet<SlotBody>>()

        renders.forEach { (_, render) ->
            val world = render.entity.world
            render.update(
                partBodies.computeIfAbsent(world) { HashSet() },
                slotBodies.computeIfAbsent(world) { HashSet() }
            )
        }

        plugin.playerState.forEach { (player, pState) ->
            val state = pState.renders
            val world = player.world
            val wPartBodies = partBodies.getOrDefault(world, HashSet())
            val wSlotBodies = slotBodies.getOrDefault(world, HashSet())

            val ray = player.eyeLocation.ray()
            val distance = settings.reach
            val bypass = state.bypassOptions

            state.selected?.let { selected ->
                if (selected.dragging.active) {
                    if (selected.dragging == NodeRenders.DragState.MOVING) {
                        selected.snapTo = raycastOf(wSlotBodies
                            .filter {
                                // don't allow non-modifiable renders to be attached to
                                (bypass || it.slot.parent.render.options().modifiable) &&
                                // if slot is not on the currently selected render
                                it.slot.parent.render != selected.part.render &&
                                // if slot is empty
                                !it.slot.parent.node.has(it.slot.key)
                            }
                        ).cast(ray, distance)?.hit?.let {
                            val slot = it.slot
                            NodeRenders.SnapTarget(
                                slot,
                                // compatible AND *slot* is modifiable (not render)
                                slot.parent.node.component.slots[slot.key]
                                    ?.compatible(selected.part.render.root.node) == true &&
                                (bypass || slot.modifiable)
                            )
                        }
                    }
                    Unit// don't skip to the `run`
                } else null // skip to the `run`
            } ?: run {
                state.selected?.snapTo = null
                if (!state.lockSelection) {
                    listOf(
                        plugin.raycast(player.world).castBlocks(ray, distance) {
                            it.fluid == null
                        },
                        raycastOf(wPartBodies.filter {
                            // don't allow non-selectable renders to be selected
                            bypass || it.part.render.options().selectable
                        }).cast(ray, distance)
                    ).closest()?.let { col ->
                        val hit = col.hit
                        if (hit is PartBody) {
                            select(state, hit.part)
                        } else null
                    } ?: deselect(state)
                }
            }

            state.selected?.let { selected ->
                val part = selected.part
                val render = part.render

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

                when (selected.dragging) {
                    NodeRenders.DragState.MOVING -> {
                        if (part.isRoot) {
                            fun computePartTransform() = computePartTransform(state, part.data.surfaceOffset, render.transform)

                            // move the entire render around
                            // or snap it to the currently selected slot
                            selected.snapTo?.let { snapTo ->
                                if (snapTo.compatible) {
                                    part.glowState(player, PartGlowState.VALID)
                                    render.transform = snapTo.slot.parent.transform + snapTo.slot.data.transform + part.data.attachedTransform
                                } else {
                                    part.glowState(player, PartGlowState.INVALID)
                                    render.transform = computePartTransform()
                                }
                            } ?: run {
                                part.glowState(player, PartGlowState.MARKED)
                                render.transform = computePartTransform()
                            }
                        } else {
                            // move the single part
                            part.data.detach?.let { (axis, maxDistance, detachDistance) ->
                                // to determine how far along part.attachAxis we transform our part,
                                // we find the intersection of a ray and a line:
                                // · p0 is the player's eye
                                // · p1 is the player's eye + player dir (basically another point along their dir line)
                                // · pCo and pNo define a plane along the part
                                //   · pCo is the part's transform
                                //   · pNo is the normal that makes the plane "point" towards the player
                                // get the intersection of these, and you can get the transform along the drag line
                                val loc = player.eyeLocation
                                val origin = loc.vector()
                                val pCo = part.baseTransform.tl
                                val pNo = (pCo - origin).normalized

                                iLinePlane(loc.vector(), loc.vector() + loc.direction(), pCo, pNo)?.let { intersect ->
                                    // the distance our intersect is along this axis (- means it's backwards)
                                    val dstMoved = part.baseTransform.invert(intersect).dot(axis)
                                    if (dstMoved >= detachDistance) {
                                        part.detach()
                                        part.play(part.data.soundDetach)
                                    } else {
                                        part.dragTransform = Transform(
                                            tl = axis * clamp(dstMoved, 0.0, maxDistance)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    NodeRenders.DragState.ROTATING -> {
                        selected.rotationPlaneNormal?.let { pNo ->
                            val loc = player.eyeLocation
                            val origin = loc.vector()
                            val pCo = render.transform.tl

                            if (state.showShapes.active) {
                                settings.particleBody?.let {
                                    state.effector.showLine(it, pCo, pCo + pNo * 2.0, settings.particleStep)
                                }
                            }

                            iLinePlane(origin, origin + loc.direction(), pCo, pNo)?.let { intersect ->
                                // `intersect` is where we want our transform to point towards
                                // TODO fix this on verticals
                                render.transform = render.transform.copy(
                                    rot = quaternionLooking((intersect - pCo).normalized, Vector3.Up)
                                )
                            }
                        }
                    }
                    NodeRenders.DragState.INVALID -> {}
                    NodeRenders.DragState.NONE -> {}
                }
            }
        }
    }

    internal fun handleSpawnEntity(player: Player, id: Int): Boolean {
        if (hideIds.contains(id)) {
            // cancel event but don't do anything
            // the ID gets removed on the next tick
            return true
        }

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
                } catch (ex: Exception) {
                    // logged under verbose because this could cause *a lot* of spam
                    // if someone has an error, they should enable verbose and see what the issue is
                    // we **do not** want to remove any data or any entities, because the data could be missing
                    // just due to a misconfiguration or something
                    plugin.log.line(LogLevel.Verbose, ex) { "Could not create render from entity at ${entity.location.vector()}" }
                    // ...but we still cancel the spawn packets being sent out
                    true
                }
            }
        } ?: false
    }

    internal fun handleDestroyEntities(player: Player, ids: IntArray) {
        ids.forEach { id ->
            renders[id]?.despawn(setOf(player))
        }
    }

    internal fun handleInput(event: PacketInputListener.Event) {
        val state = plugin.playerState(event.player).renders
        state.selected?.let { selected ->
            val part = selected.part
            val bypass = state.bypassOptions

            fun release() {
                val snapTo = selected.snapTo
                if (snapTo?.compatible == true) {
                    part.play(part.data.soundAttach)
                    plugin.scheduleDelayed {
                        part.attachTo(snapTo.slot)
                    }
                }
                dragging(state, NodeRenders.DragState.NONE)
            }

            fun move() {
                if (bypass || part.render.options().movable) {
                    dragging(state,
                        // if root OR parent slot is modifiable
                        if (part.parent == null || bypass || part.parent?.slot?.modifiable == true) NodeRenders.DragState.MOVING
                        else NodeRenders.DragState.INVALID)
                }
            }

            fun rotate() {
                if (bypass || part.render.options().rotatable) {
                    dragging(state, NodeRenders.DragState.ROTATING)
                }
            }

            fun grab() {
                if (part.isRoot && (bypass || part.render.options().grabbable)) {
                    val inv = state.player.inventory
                    val slot = inv.heldItemSlot
                    if (inv.getItem(slot).isEmpty()) {
                        plugin.persistence.stateToStack(
                            holderByPlayer(plugin.hostOf(state.player), state.player, slot),
                            paperStateOf(part.node)
                        ).let { stack ->
                            part.play(part.data.soundGrab)
                            state.selected = null
                            plugin.scheduleDelayed {
                                part.render.remove()
                                state.player.inventory.setItem(slot, stack)
                            }
                        }
                    }
                }
            }

            settings.inputs.actionOf(event.input, event.player)?.let {
                when (it) {
                    MOVE_TOGGLE -> {
                        if (selected.dragging.active) release()
                        else move()
                    }
                    MOVE_START -> move()
                    ROTATE_TOGGLE -> {
                        if (selected.dragging.active) release()
                        else rotate()
                    }
                    ROTATE_START -> rotate()
                    DRAG_STOP -> release()
                    GRAB -> grab()
                }
                event.cancel()
            }
        }
    }

    fun partOf(node: PaperDataNode, parent: PartKey? = null): Part {
        val feature = node.component.features.by<RenderFeature.Profile<*>>(RenderFeature)
            ?: throw IllegalArgumentException(node.errorMsg("No feature '${RenderFeature.id}' to create render of"))
        return Part(node,
            parent,
            HashMap(),
        ).also { part ->
            node.component.slots.forEach { (key) ->
                part.slots[key] = PartSlot(
                    part,
                    key,
                    feature.joints[key]
                        ?: throw IllegalArgumentException(node.errorMsg("Feature '${RenderFeature.id}' does not have slot transform for '$key'")),
                    node.node(key)?.let { child ->
                        partOf(child, PartKey(part, key))
                    }
                )
            }
            part.setupNode(node)
        }
    }

    fun create(entity: Entity, part: Part, rotation: Quaternion, options: NodeRenders.Options? = null, hideSpawn: Boolean = false): Render {
        return Render(part, entity, rotation, options).also {
            // initialize transforms
            it.update(null, null)
            // initialize persistence
            it.save()

            fun Part.apply() {
                render = it
                slots.forEach { (_, slot) -> slot.part?.apply() }
            }
            it.root.apply()

            if (!hideSpawn) {
                it.root.setupMeshes()
            }

            renders[it.id] = it
        }
    }

    fun create(part: Part, world: World, transform: Transform, options: NodeRenders.Options? = null, hideSpawn: Boolean = false): Render {
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
            render = create(entity, part, transform.rot, options, hideSpawn)
            if (hideSpawn) {
                hideIds.add(entity.entityId)
            }
        }
        return render ?: throw IllegalStateException("Render entity spawned but not returned (delayed entity spawn?)")
    }

    override fun create(node: PaperDataNode, world: World, transform: Transform, options: NodeRenders.Options?): Render {
        return create(partOf(node), world, transform, options, false)
    }

    fun loadRender(entity: Entity): Render? {
        return plugin.persistence.holderOf(entity.persistentDataContainer)[kRenderNode]?.let { tag ->
            val node = plugin.persistence.nodeOf(tag)
                ?: throw IllegalArgumentException("Entity has tag $kRenderNode but it was not a valid node")
            val rotation = entity.persistentDataContainer.getOrDefault(kRotation, QuaternionDataType, Quaternion.Identity)
            val options = entity.persistentDataContainer.getOrDefault(kOptions, PersistentDataType.INTEGER, -1)
            create(entity, partOf(node),
                rotation,
                if (options < 0) settings.defaultOptions else NodeRenders.Options.of(options)
            )
        }
    }

    fun remove(id: Int, despawn: Boolean = true) {
        renders.remove(id)?.remove(despawn)
    }

    override fun select(state: PlayerState, part: Part) {
        state.selected?.let { if (part === it.part) return }
        deselect(state)
        state.selected = NodeRenders.Selection(part)
        part.showSelect(state.player)
    }

    override fun deselect(state: PlayerState) {
        state.selected?.let {
            dragging(state, NodeRenders.DragState.NONE)
            it.part.showDeselect(state.player)
            it.part.undoGlowState(state.player)
            state.selected = null
        }
    }

    override fun dragging(state: PlayerState, dragging: NodeRenders.DragState) {
        state.selected?.let { selected ->
            val part = selected.part
            val old = selected.dragging
            if (old == dragging) return

            selected.dragging = dragging
            state.lastNormal = null
            part.glowState(state.player,
                when {
                    dragging == NodeRenders.DragState.INVALID -> PartGlowState.INVALID
                    dragging.active -> PartGlowState.MARKED
                    else -> PartGlowState.DEFAULT
                }
            )

            part.play(
                if (!old.active && dragging.active) part.data.soundDragStart
                else part.data.soundDragStop)

            if (dragging == NodeRenders.DragState.ROTATING) {
                selected.rotationPlaneNormal = (part.render.transform.rot * settings.rotation.inverse) * Vector3.Forward
            }

            if (!dragging.active) {
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
        val options: NodeRenders.Options?,
    ) : NodeRender<Part> {
        val id = entity.entityId
        override val world get() = entity.world
        override var transform: Transform
            get() = entity.transform.copy(rot = rotation)
            set(value) {
                rotation = value.rot
                entity.transform = value
            }

        fun options() = options ?: settings.defaultOptions

        internal var lastTracked: Collection<PlayerState> = emptySet()

        internal fun update(
            partBodies: MutableCollection<PartBody>? = null,
            slotBodies: MutableCollection<SlotBody>? = null
        ) {
            lastTracked = entity.trackedPlayers.map { plugin.playerState(it).renders }
            fun Part.apply(tf: Transform) {
                baseTransform = tf + data.localTransform
                transform = baseTransform + dragTransform

                fun renderTo(state: PlayerState) = when (state.showShapes) {
                    ShowShapes.NONE -> false
                    ShowShapes.PART -> state.selected?.part == this
                    ShowShapes.RENDER -> state.selected?.part?.render == this@Render
                    ShowShapes.ALL -> true
                }

                this.bodies.forEach { body ->
                    val bodyTf = transform + body.transform
                    partBodies?.add(PartBody(body.shape, bodyTf, this))
                    lastTracked.forEach { state ->
                        if (renderTo(state)) {
                            settings.particleBody?.let {
                                state.effector.showShape(it, body.shape, bodyTf)
                            }
                        }
                    }
                }

                meshes.forEach { mesh ->
                    val meshTf = mesh.computeTransform(transform)
                    mesh.sendTransform(lastTracked.map { it.player }, meshTf)
                }
                slots.forEach { (_, slot) ->
                    val slotTf = transform + slot.data.transform
                    slot.data.bodies.forEach { body ->
                        val bodyTf = slotTf + body.transform
                        slotBodies?.add(SlotBody(body.shape, bodyTf, slot))
                        lastTracked.forEach { state ->
                            if (renderTo(state)) {
                                settings.particleSlot?.let {
                                    state.effector.showShape(it, body.shape, bodyTf)
                                }
                            }
                        }
                    }
                    slot.part?.let {
                        it.apply(transform + slot.data.transform + it.data.attachedTransform)
                    }
                }
            }
            root.apply(transform)

            val axesFrom = transform.tl
            val toX = axesFrom + (transform.rot * Vector3.Right)
            val toY = axesFrom + (transform.rot * Vector3.Up)
            val toZ = axesFrom + (transform.rot * Vector3.Forward)
            val step = settings.particleStep

            lastTracked.forEach { state ->
                if (when (state.showShapes) {
                    ShowShapes.NONE -> false
                    ShowShapes.PART, ShowShapes.RENDER -> state.selected?.part?.render == this
                    ShowShapes.ALL -> true
                }) {
                    settings.particleAxes?.let {
                        state.effector.showLine(it.x, axesFrom, toX, step)
                        state.effector.showLine(it.y, axesFrom, toY, step)
                        state.effector.showLine(it.z, axesFrom, toZ, step)
                    }
                }
            }
        }

        fun saveRotation() {
            entity.persistentDataContainer.set(kRotation, QuaternionDataType, rotation)
        }

        fun saveNode() {
            plugin.persistence.holderOf(entity.persistentDataContainer)[kRenderNode] =
                plugin.persistence.newTag().apply { root.node.serialize(this) }
        }

        fun save() {
            saveRotation()
            saveNode()
            options?.let { entity.persistentDataContainer.set(kOptions, PersistentDataType.INTEGER, it.value) }
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

        fun despawn(players: Iterable<Player>) {
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

        fun despawn() {
            // if the entity is already removed, `trackedPlayers` will give an empty set
            despawn(lastTracked.map { it.player })
        }

        fun remove(despawn: Boolean = true) {
            renders.remove(id)
            entity.remove()
            if (despawn) {
                despawn()
            }
        }

        override fun remove() {
            remove(true)
        }
    }

    enum class PartGlowState {
        DEFAULT,
        MARKED,
        VALID,
        INVALID,
    }

    data class PartKey(
        val part: Part,
        val key: String,
    ) {
        val slot get() = part.slots[key]
    }

    inner class Part(
        node: PaperDataNode,
        var parent: PartKey?,
        override var slots: MutableMap<String, PartSlot<Part>>,
    ) : NodePart {
        override var node: PaperDataNode = node
            set(value) {
                field = value
                setupNode()
                setupMeshes(render.lastTracked.map { it.player })
            }

        val isRoot get() = parent == null

        private fun featureOf(node: PaperDataNode) =
            node.component.features.by<RenderFeature.Profile<*>>(RenderFeature)
                ?: throw IllegalArgumentException(node.errorMsg("No feature '${RenderFeature.id}' to set part parameters from"))

        internal fun setupNode() {
            val feature = featureOf(node)

            bodies = feature.bodies
            data = feature.data

            slots = node.component.slots.map { (key) ->
                val slotData = feature.joints[key]
                    ?: throw IllegalArgumentException(node.errorMsg("Feature '${RenderFeature.id}' does not have slot data for '$key'"))
                key to PartSlot(
                    this,
                    key,
                    slotData,
                    slots[key]?.part?.let { existing ->
                        // if the node we just set to, has a child for this key,
                        // we assign the existing part this new node
                        // else we just remove the part
                        node.node(key)?.let {
                            existing.setupNode(it)
                            existing
                        }
                    }
                )
            }.associate { it }.toMutableMap()
        }

        internal fun setupMeshes(players: Iterable<Player>? = null) {
            val feature = featureOf(node)

            // remove old meshes if we have players to remove them for
            val ids = meshes.map { it.entityId }.toIntArray()
            players?.forEach {
                it.sendPacket(WrapperPlayServerDestroyEntities(*ids))
            }

            val item by lazy {
                try {
                    plugin.persistence.stateToStack(
                        holderBy(plugin.hostOf(render)),
                        paperStateOf(node.copy().apply { removeChildren() })
                    ) ?: throw HostCreationException("Node does not have item hoster feature")
                } catch (ex: HostCreationException) {
                    throw RuntimeException(node.errorMsg("Could not create dynamic mesh"), ex)
                }
            }
            meshes = feature.meshes.map {
                Mesh(
                    node, it, bukkitNextEntityId, UUID.randomUUID(),
                    when (it) {
                        is RenderMeshData.Static -> it.item.asStack()
                        is RenderMeshData.Dynamic -> {
                            // we don't want any children to be rendered on this node
                            // e.g. if a root node changes model when it is complete
                            item
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

            slots.forEach { (_, slot) ->
                slot.part?.setupMeshes(players)
            }
        }

        override lateinit var bodies: Collection<Body>
        var meshes: Collection<Mesh> = emptySet()
        lateinit var data: RenderPartData
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

        fun showSelect(player: Player) {
            meshes.forEach { it.sendGlow(player, true) }
        }

        fun showDeselect(player: Player) {
            meshes.forEach { it.sendGlow(player, false) }
        }

        fun undoGlowState(player: Player) {
            lastGlowState?.let { state ->
                meshes.forEach { it.sendGlowState(player, state, false) }
            }
            lastGlowState = null
        }

        fun glowState(player: Player, state: PartGlowState) {
            lastGlowState?.let { if (state == it) return }
            undoGlowState(player)
            meshes.forEach { it.sendGlowState(player, state, true) }
            lastGlowState = state
        }

        fun detach() {
            // detach
            parent?.let {
                it.part.node.remove(it.key)
                it.part.slots[it.key]?.part = null
            } ?: throw IllegalStateException("Attempting to detach root part")
            render.saveNode()
            node.detach()

            // re-attach to a new render
            parent = null
            // use
            render = create(this, render.entity.world, transform, render.options, true)
            dragTransform = Transform.Identity
        }

        fun attachTo(slot: PartSlot<Part>) {
            if (parent != null)
                throw IllegalStateException("Attempting to attach to slot as non-root part")

            // don't despawn the virtual entities - they'll be managed by our new parent anyway
            render.remove(false)

            // attach to our new render
            val parent = slot.parent
            this.parent = PartKey(parent, slot.key)
            this.render = parent.render
            parent.node.node(slot.key, node)
            node.attach(parent.node, slot.key)
            parent.slots[slot.key]?.let {
                it.part = this
            } ?: throw IllegalStateException("Attempting to attach to non-existent slot '${slot.key}' of '${parent.node.component.id}'")
            parent.render.saveNode()

        }
    }

    data class Mesh(
        val node: PaperDataNode,
        val backing: RenderMeshData,
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
                PartGlowState.VALID -> TEAM_VALID
                PartGlowState.INVALID -> TEAM_INVALID
            }, if (add) WrapperPlayServerTeams.TeamMode.ADD_ENTITIES else WrapperPlayServerTeams.TeamMode.REMOVE_ENTITIES,
            Optional.empty(), uuid.toString()))
        }
    }
}
