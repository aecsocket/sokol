package com.github.aecsocket.sokol.paper

import com.github.aecsocket.alexandria.core.LogLevel
import com.github.aecsocket.alexandria.core.LogList
import com.github.aecsocket.alexandria.core.extension.force
import com.github.aecsocket.alexandria.core.extension.register
import com.github.aecsocket.alexandria.core.keyed.MutableRegistry
import com.github.aecsocket.alexandria.core.keyed.Registry
import com.github.aecsocket.alexandria.paper.extension.forEach
import com.github.aecsocket.alexandria.paper.extension.scheduleRepeating
import com.github.aecsocket.alexandria.paper.packet.PacketInputListener
import com.github.aecsocket.alexandria.paper.plugin.BasePlugin
import com.github.aecsocket.sokol.core.Feature
import com.github.aecsocket.sokol.core.event.TestEvent
import com.github.aecsocket.sokol.paper.feature.TestFeature
import com.github.aecsocket.sokol.paper.serializer.PaperComponentSerializer
import com.github.aecsocket.sokol.paper.serializer.PaperNodeSerializer
import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerPriority
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
import org.bukkit.NamespacedKey
import org.bukkit.entity.Item
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataHolder
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ObjectMapper
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializerCollection

const val REPR_WORLD = "world"
const val REPR_CHUNK = "chunk"
const val REPR_ENTITY = "entity"
const val REPR_ITEM = "item"

class SokolPlugin : BasePlugin() {
    lateinit var settings: SokolSettings
    lateinit var persistence: SokolPersistence
    lateinit var keyNode: NamespacedKey
    lateinit var keyOnTick: NamespacedKey

    private val _lastRepresentatives = HashMap<String, Pair<Int, Int>>()
    val lastRepresentatives: Map<String, Pair<Int, Int>>
        get() = _lastRepresentatives

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
        keyOnTick = key("on_tick")
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

        scheduleRepeating {
            _lastRepresentatives.clear()

            // god I despise bukkit nullability annotations
            fun represent(
                pdh: PersistentDataHolder,
                type: String
            ) {
                val pdc = pdh.persistentDataContainer
                val isActual = pdc.has(keyNode) && pdc.has(keyOnTick)
                if (isActual) {
                    persistence.get(pdh.persistentDataContainer)?.let { node ->
                        val state = node.createState()
                        state.callEvent { TestEvent(it, 12345) }
                    }
                    // TODO logic to deserialize all this

                }
                _lastRepresentatives[type] = (_lastRepresentatives[type] ?: (0 to 0)).let { (possible, actual) ->
                    possible + 1 to if (isActual) actual + 1 else actual
                }
            }

            fun represent(stack: ItemStack?) {
                stack?.itemMeta?.let { represent(it, REPR_ITEM) }
            }

            server.worlds.forEach { world ->
                represent(world, REPR_WORLD)
                world.loadedChunks.forEach { chunk ->
                    represent(chunk, REPR_CHUNK)
                }
                world.entities.forEach { entity ->
                    represent(entity, REPR_ENTITY)
                    when (entity) {
                        is InventoryHolder -> entity.inventory.forEach { represent(it) }
                        is LivingEntity -> entity.equipment?.forEach { represent(it) }
                        is Item -> represent(entity.itemStack)
                        is ItemFrame -> represent(entity.item)
                    }
                }
            }
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
