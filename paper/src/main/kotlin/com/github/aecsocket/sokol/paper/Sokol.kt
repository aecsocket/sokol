package com.github.aecsocket.sokol.paper

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent
import com.github.aecsocket.alexandria.core.DoubleMod
import com.github.aecsocket.alexandria.core.IntMod
import com.github.aecsocket.alexandria.core.LogLevel
import com.github.aecsocket.alexandria.core.LogList
import com.github.aecsocket.alexandria.core.effect.ParticleEffect
import com.github.aecsocket.alexandria.core.effect.SoundEffect
import com.github.aecsocket.alexandria.core.extension.force
import com.github.aecsocket.alexandria.core.extension.register
import com.github.aecsocket.alexandria.core.extension.registerExact
import com.github.aecsocket.alexandria.core.keyed.MutableRegistry
import com.github.aecsocket.alexandria.core.keyed.Registry
import com.github.aecsocket.alexandria.core.physics.*
import com.github.aecsocket.alexandria.core.serializer.*
import com.github.aecsocket.alexandria.paper.effect.PaperEffectors
import com.github.aecsocket.alexandria.paper.extension.bukkitPlayers
import com.github.aecsocket.alexandria.paper.extension.registerEvents
import com.github.aecsocket.alexandria.paper.extension.scheduleRepeating
import com.github.aecsocket.alexandria.paper.input.InputMapper
import com.github.aecsocket.alexandria.paper.input.PacketInputListener
import com.github.aecsocket.alexandria.paper.physics.PaperRaycast
import com.github.aecsocket.alexandria.paper.plugin.BasePlugin
import com.github.aecsocket.alexandria.paper.plugin.ConfigOptionsAction
import com.github.aecsocket.alexandria.paper.serializer.*
import com.github.aecsocket.glossa.configurate.I18NSerializers
import com.github.aecsocket.sokol.core.NodePath
import com.github.aecsocket.sokol.core.SokolPlatform
import com.github.aecsocket.sokol.core.rule.Rule
import com.github.aecsocket.sokol.core.serializer.*
import com.github.aecsocket.sokol.core.stat.ApplicableStats
import com.github.aecsocket.sokol.core.stat.StatMap
import com.github.aecsocket.sokol.core.util.RenderMesh
import com.github.aecsocket.sokol.paper.feature.PaperItemHost
import com.github.aecsocket.sokol.paper.feature.PaperRender
import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerPriority
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
import org.bukkit.Color
import org.bukkit.GameMode
import org.bukkit.Particle
import org.bukkit.World
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.world.EntitiesUnloadEvent
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ObjectMapper
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializerCollection
import java.util.*
import kotlin.collections.HashMap

private const val COMPONENT = "component"
private const val BLUEPRINT = "blueprint"

class Sokol : BasePlugin<Sokol.LoadScope>(),
    SokolPlatform<PaperComponent, PaperBlueprint, PaperFeature, PaperDataNode> {
    interface LoadScope : BasePlugin.LoadScope {
        val features: MutableRegistry<PaperFeature>
    }

    override fun createLoadScope(configOptionActions: MutableList<ConfigOptionsAction>) = object : LoadScope {
        override val features: MutableRegistry<PaperFeature>
            get() = _features

        override fun onConfigOptionsSetup(action: ConfigOptionsAction) {
            configOptionActions.add(action)
        }
    }

    private val hostResolver = HostResolver(this, this::onHostResolve)
    lateinit var settings: SokolSettings
    override val persistence = PaperPersistence(this)
    val renders = DefaultNodeRenders(this)
    val effectors = PaperEffectors()
    val statMapSerializer = StatMapSerializer()
    val ruleSerializer = RuleSerializer()

    var lastHosts: Map<HostType, HostsResolved> = emptyMap()
        private set

    @get:Synchronized private val _playerState = HashMap<Player, PlayerState>()
    val playerState: Map<Player, PlayerState> = _playerState

    private val _features = Registry.create<PaperFeature>()
    override val features: Registry<PaperFeature> get() = _features

    private val _blueprints = Registry.create<PaperBlueprint>()
    override val blueprints: Registry<PaperBlueprint> get() = _blueprints

    private val _components = Registry.create<PaperComponent>()
    override val components: Registry<PaperComponent> get() = _components

    fun playerState(player: Player) = _playerState.computeIfAbsent(player) { PlayerState(this, it) }

    override fun nodeOf(component: PaperComponent) = PaperDataNode(component)

    // centralized raycast instances
    fun raycast(world: World) = PaperRaycast(world)

    override fun onLoad() {
        super.onLoad()
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this))
        PacketEvents.getAPI().settings
            .checkForUpdates(false)
            .bStats(true)
        PacketEvents.getAPI().load()
    }

    override fun onEnable() {
        super.onEnable()
        PacketEvents.getAPI().eventManager.apply {
            registerListener(SokolPacketListener(this@Sokol))
            registerListener(PacketInputListener { onInputReceived(it) }, PacketListenerPriority.NORMAL)
        }
        PacketEvents.getAPI().init()
        SokolCommand(this)
        registerEvents(object : Listener {
            @EventHandler
            fun PlayerJoinEvent.on() {
                playerState(player)
            }

            @EventHandler
            fun PlayerQuitEvent.on() {
                _playerState.remove(player)
            }

            @EventHandler
            fun EntityRemoveFromWorldEvent.on() {
                renders.remove(entity.entityId)
            }

            @EventHandler
            fun EntitiesUnloadEvent.on() {
                entities.forEach { renders.remove(it.entityId) }
            }
        })
        effectors.init(this)
        renders.init()

        val plugin = this
        onLoad {
            features.register(PaperItemHost(plugin))
            features.register(PaperRender(plugin))
        }
    }

    override fun serverLoad(): Boolean {
        if (super.serverLoad()) {
            scheduleRepeating {
                lastHosts = if (settings.hostResolution.enabled) hostResolver.resolve() else emptyMap()
                bukkitPlayers.forEach { playerState(it).tick() }
                renders.update()
            }
            return true
        }
        return false
    }

    override fun onDisable() {
        super.onDisable()
        PacketEvents.getAPI().terminate()
    }

    override fun setupConfigOptions(
        serializers: TypeSerializerCollection.Builder,
        mapper: ObjectMapper.Factory.Builder
    ) {
        super.setupConfigOptions(serializers, mapper)
        serializers
            .registerExact(Quaternion::class, QuaternionSerializer())
            .registerExact(NodePath::class, NodePathSerializer)
            .registerExact(RenderMesh::class, RenderMeshSerializer)
            .registerExact(PaperComponent::class, PaperComponentSerializer(this))
            .registerExact(PaperDataNode::class, PaperNodeSerializer(this))
            .registerExact(PaperBlueprint::class, PaperBlueprintSerializer(this))
            .registerExact(StatMap::class, statMapSerializer)
            .registerExact(ApplicableStats::class, ApplicableStatsSerializer)
            .register(Rule::class, ruleSerializer)
    }

    override fun loadInternal(log: LogList, settings: ConfigurationNode): Boolean {
        if (super.loadInternal(log, settings)) {
             try {
                 this.settings = settings.force()
            } catch (ex: SerializationException) {
                log.line(LogLevel.Error, ex) { "Could not load settings" }
                 return false
            }

            SokolPlatform.loadRegistry(log, this::loaderBuilder, _components, dataFolder.resolve(COMPONENT), PaperComponent::class.java)
            SokolPlatform.loadRegistry(log, this::loaderBuilder, _blueprints, dataFolder.resolve(BLUEPRINT), PaperBlueprint::class.java)

            hostResolver.load()
            renders.load(settings)

            return true
        }
        return false
    }

    private fun onHostResolve(state: PaperTreeState, host: PaperNodeHost) {
        state.callEvent(host, PaperNodeEvent.OnTick)
    }

    private fun onInputReceived(event: PacketInputListener.Event) {
        val player = event.player
        if (player.gameMode == GameMode.SPECTATOR || !player.isValid) return
        renders.handleInput(event)

        persistence.nodeTagOf(player.persistentDataContainer)?.let { tag ->
            persistence.nodeOf(tag)?.let { node ->
                val state = paperStateOf(node)
                // todo make player host
                // call events
                persistence.stateInto(state, tag)
            }
        }

        val host = PaperNodeHost.OfEntity(player)
        player.inventory.forEachIndexed { idx, stack -> stack?.let {
            persistence.nodeTagOf(stack)?.let { tag ->
                persistence.nodeOf(tag)?.let { node ->
                    val state = paperStateOf(node)
                    val meta by lazy { stack.itemMeta }
                    var dirty = false
                    PaperNodeHost.OfWritableStack(
                        StackHolder.byPlayer(host, player, idx),
                        { stack },
                        { meta },
                        { dirty = true }
                    ).apply {
                        state.callEvent(this, PaperNodeEvent.OnInput(event.input, player))
                    }

                    if (dirty) {
                        // full update; write meta
                        //  · we write into PDC first, so when meta is set onto stack,
                        //    meta's PDC is written into stack's tag
                        //  · we can't just write meta then apply to our local `tag`,
                        //    because setItemMeta overwrites the stack's CompoundTag
                        persistence.nodeTagTo(
                            persistence.newTag().apply { persistence.stateInto(state, this) },
                            meta.persistentDataContainer,
                        )
                        stack.itemMeta = meta
                    } else {
                        // write directly to stack tag; avoid itemMeta write
                        persistence.stateInto(state, tag)
                    }
                }
            }
        } }
    }
}
