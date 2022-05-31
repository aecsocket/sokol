package com.github.aecsocket.sokol.paper

import com.github.aecsocket.alexandria.core.LogLevel
import com.github.aecsocket.alexandria.core.LogList
import com.github.aecsocket.alexandria.core.extension.force
import com.github.aecsocket.alexandria.core.extension.register
import com.github.aecsocket.alexandria.core.keyed.Registry
import com.github.aecsocket.alexandria.paper.ElementResolver
import com.github.aecsocket.alexandria.paper.ElementType
import com.github.aecsocket.alexandria.paper.ServerElement
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
import net.kyori.adventure.text.Component.translatable
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.NamespacedKey
import org.bukkit.block.TileState
import org.bukkit.persistence.PersistentDataHolder
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ObjectMapper
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializerCollection

class SokolPlugin : BasePlugin() {
    lateinit var settings: SokolSettings
    lateinit var persistence: SokolPersistence
    lateinit var keyNode: NamespacedKey
    lateinit var keyTick: NamespacedKey

    var lastHosts: Map<ElementType, ElementResolver.ElementsResolved> = emptyMap()
        private set

    private val _components = Registry.create<PaperComponent>()
    val components: Registry<PaperComponent>
        get() = _components

    private val _features = Registry.create<PaperFeature>()
    val features: Registry<PaperFeature>
        get() = _features

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
        persistence = SokolPersistence(this)
        keyNode = key("node")
        keyTick = key("tick")
        PacketEvents.getAPI().eventManager.apply {
            registerListener(SokolPacketListener(this@SokolPlugin))
            registerListener(PacketInputListener { event ->
                event.player.inventory.forEach { stack ->
                    persistence.getStack(stack)?.let { node ->
                        // TODO fwd inputs to node
                    }
                }
            }, PacketListenerPriority.NORMAL)
        }
        PacketEvents.getAPI().init()
        SokolCommand(this)

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

        val resolver = ElementResolver(
            keyTick,
            settings.hostResolution.containerItems,
            settings.hostResolution.containerBlocks
        ) { element ->
            val host = when (element) {
                is ServerElement.OfStack -> {
                    val meta = element.stack.itemMeta
                    PaperNodeHost.fromStack(element, meta)
                    // todo use this meta later
                }
                else -> PaperNodeHost.from(element)
            }
            val pdc = host.pdc
            persistence.get(pdc)?.let { node ->
                node.createState(host).callEvent { NodeEvent.Tick(it) }
            } ?: run {
                log.line(LogLevel.WARNING) { "Host $host was marked as ticking but is not node - removed tick key" }
                pdc.remove(keyTick)
            }
        }

        scheduleRepeating {
            lastHosts = if (settings.hostResolution.enabled) resolver.resolve() else emptyMap()
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
            return true
        }
        return false
    }
}
