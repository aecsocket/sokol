package com.github.aecsocket.sokol.paper

import com.github.aecsocket.alexandria.core.LogLevel
import com.github.aecsocket.alexandria.core.LogList
import com.github.aecsocket.alexandria.core.extension.force
import com.github.aecsocket.alexandria.core.extension.register
import com.github.aecsocket.alexandria.core.keyed.Keyed
import com.github.aecsocket.alexandria.core.keyed.MutableRegistry
import com.github.aecsocket.alexandria.core.keyed.Registry
import com.github.aecsocket.alexandria.paper.extension.registerEvents
import com.github.aecsocket.alexandria.paper.extension.scheduleRepeating
import com.github.aecsocket.alexandria.paper.packet.PacketInputListener
import com.github.aecsocket.alexandria.paper.plugin.BasePlugin
import com.github.aecsocket.sokol.core.event.NodeEvent
import com.github.aecsocket.sokol.paper.feature.TestFeature
import com.github.aecsocket.sokol.paper.serializer.PaperComponentSerializer
import com.github.aecsocket.sokol.paper.serializer.PaperNodeSerializer
import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerPriority
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ObjectMapper
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializerCollection

private const val COMPONENTS = "components"
private const val BLUEPRINTS = "blueprints"

class SokolPlugin : BasePlugin() {
    private val playerData = HashMap<Player, PlayerData>()
    lateinit var settings: SokolSettings
    lateinit var keyNode: NamespacedKey
    lateinit var keyTick: NamespacedKey
    lateinit var persistence: SokolPersistence
    private lateinit var hostResolver: HostResolver

    var lastHosts: Map<HostType, HostsResolved> = emptyMap()
        private set

    private val _features = Registry.create<PaperFeature>()
    val features: Registry<PaperFeature>
        get() = _features

    private val _blueprints = Registry.create<PaperBlueprint>()
    val blueprints: Registry<PaperBlueprint>
        get() = _blueprints

    private val _components = Registry.create<PaperComponent>()
    val components: Registry<PaperComponent>
        get() = _components

    internal fun playerData(player: Player) = playerData.computeIfAbsent(player) { PlayerData(this, it) }

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
        keyNode = key("node")
        keyTick = key("tick")
        persistence = SokolPersistence(this)
        hostResolver = HostResolver(this, this::onHostResolve)
        PacketEvents.getAPI().eventManager.apply {
            registerListener(SokolPacketListener(this@SokolPlugin))
            registerListener(PacketInputListener(this@SokolPlugin::onInputReceived), PacketListenerPriority.NORMAL)
        }

        PacketEvents.getAPI().init()
        SokolCommand(this)
        registerEvents(object : Listener {
            @EventHandler
            fun onQuit(event: PlayerQuitEvent) {
                playerData.remove(event.player)
            }
        })

        // todo proper registration system
        _components.register(
            PaperComponent("some_component", mapOf(
                TestFeature.ID to TestFeature(this).Profile("abc 123")
            ), emptyMap())
        )
        _features.register(TestFeature(this))
    }

    override fun serverLoad() {
        super.serverLoad()

        scheduleRepeating {
            lastHosts = if (settings.hostResolution.enabled) hostResolver.resolve() else emptyMap()
            playerData.forEach { (_, data) -> data.tick() }
        }
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
            .register(PaperComponent::class, PaperComponentSerializer(this))
            .register(PaperDataNode::class, PaperNodeSerializer(this))
    }

    override fun loadInternal(log: LogList, settings: ConfigurationNode): Boolean {
        if (super.loadInternal(log, settings)) {
             try {
                 this.settings = settings.force()
            } catch (ex: SerializationException) {
                log.line(LogLevel.ERROR, ex) { "Could not load settings" }
                 return false
            }
            this.settings.hostResolution.apply {
                hostResolver.containerItems = containerItems
                hostResolver.containerBlocks = containerBlocks
            }

            //loadRegistry(_components, COMPONENTS)
            return true
        }
        return false
    }

    private fun <T : Keyed> loadRegistry(registry: MutableRegistry<T>, path: String) {
        registry.clear()
        dataFolder.resolve(path).walkTopDown().onEnter { file ->
            if (!file.isDirectory) {

            }
            true
        }
    }

    private fun onHostResolve(state: PaperTreeState, host: PaperNodeHost) {
        state.callEvent(host, NodeEvent.OnTick())
    }

    private fun onInputReceived(event: PacketInputListener.Event) {
        val player = event.player
        persistence.dataToTag(player.persistentDataContainer)?.let { tag ->
            val state = persistence.tagToState(tag)
            // todo make player host
            // call events
            persistence.stateToTag(state, tag)
        }

        val host = PaperNodeHost.OfEntity(player)
        player.inventory.forEachIndexed { idx, stack -> stack?.let {
            persistence.stackToTag(stack)?.let { tag ->
                val state = persistence.tagToState(tag)
                val meta by lazy { stack.itemMeta }
                var dirty = false
                PaperNodeHost.OfWritableStack(
                    StackHolder.byPlayer(host, player, idx),
                    { stack },
                    { meta },
                    { dirty = true }
                ).apply {
                    state.callEvent(this, NodeEvent.OnInput(event.input))
                }

                if (dirty) {
                    // full update; write meta
                    //  · we write into PDC first, so when meta is set onto stack,
                    //    meta's PDC is written into stack's tag
                    //  · we can't just write meta then apply to our local `tag`,
                    //    because setItemMeta overwrites the stack's CompoundTag
                    persistence.tagToData(
                        persistence.newTag().apply { persistence.stateToTag(state, this) },
                        meta.persistentDataContainer)
                    stack.itemMeta = meta
                } else {
                    // write directly to stack tag; avoid itemMeta write
                    persistence.stateToTag(state, tag)
                }
            }
        } }
    }
}
