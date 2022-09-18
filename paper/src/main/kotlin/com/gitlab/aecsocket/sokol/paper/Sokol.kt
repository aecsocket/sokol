package com.gitlab.aecsocket.sokol.paper

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent
import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import com.gitlab.aecsocket.alexandria.core.LogLevel
import com.gitlab.aecsocket.alexandria.core.LogList
import com.gitlab.aecsocket.alexandria.core.extension.*
import com.gitlab.aecsocket.alexandria.core.keyed.Registry
import com.gitlab.aecsocket.alexandria.paper.AlexandriaAPI
import com.gitlab.aecsocket.alexandria.paper.BasePlugin
import com.gitlab.aecsocket.alexandria.paper.extension.registerEvents
import com.gitlab.aecsocket.alexandria.paper.extension.scheduleRepeating
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.feature.ColliderComponent
import com.gitlab.aecsocket.sokol.paper.feature.ColliderSystem
import com.gitlab.aecsocket.sokol.paper.feature.MeshSystem
import io.github.retrooper.packetevents.util.SpigotReflectionUtil
import net.kyori.adventure.key.Key
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.spongepowered.configurate.ConfigurateException
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.serialize.SerializationException
import java.nio.file.FileVisitResult
import kotlin.io.path.isRegularFile

private const val BLUEPRINT = "blueprint"
private const val ITEMS = "items"
private const val ENTITIES = "entities"

private lateinit var instance: Sokol
val SokolAPI get() = instance

class Sokol : BasePlugin() {
    @ConfigSerializable
    data class Settings(
        val enabled: Boolean = false
    )

    lateinit var settings: Settings private set

    private val _componentTypes = HashMap<String, SokolComponentType>()
    val componentTypes: Map<String, SokolComponentType> get() = _componentTypes

    private val _itemBlueprints = Registry.create<ItemBlueprint>()
    val itemBlueprints: Registry<ItemBlueprint> get() = _itemBlueprints

    private val _entityBlueprints = Registry.create<EntityBlueprint>()
    val entityBlueprints: Registry<EntityBlueprint> get() = _entityBlueprints

    val engine = SokolEngine()
    val persistence = SokolPersistence(this)
    val entityResolver = EntityResolver(this)

    init {
        instance = this
    }

    val hostableByItem = HostableByItem()

    override fun onEnable() {
        super.onEnable()
        SokolCommand(this)
        AlexandriaAPI.registerConsumer(this,
            onInit = {
                serializers
                    .register(SokolComponent.Persistent::class, ComponentSerializer(this@Sokol))
                    .registerExact(ItemBlueprint::class, ItemBlueprintSerializer(this@Sokol))
                    .registerExact(EntityBlueprint::class, EntityBlueprintSerializer(this@Sokol))
            },
            onLoad = {
                addDefaultI18N()
            }
        )

        engine.addSystem(ColliderSystem(this))
        engine.addSystem(MeshSystem(this))

        registerComponentType(hostableByItem)
        registerComponentType(HostableByEntity())
        registerComponentType(ColliderComponent())

        scheduleRepeating {
            val entities = HashSet<SokolEntity>()
            entityResolver.resolve { entities.add(it) }
            engine.update(entities)
        }

        registerEvents(object : Listener {
            @EventHandler
            fun EntityAddToWorldEvent.on() {
                val mob = entity
                updateEntity(mob) { entity ->
                    engine.call(setOf(entity), ByEntityEvent.Added(this))
                }
            }

            @EventHandler
            fun EntityRemoveFromWorldEvent.on() {
                val mob = entity
                updateEntity(mob) { entity ->
                    engine.call(setOf(entity), ByEntityEvent.Removed(this))
                }
            }
        })

        PacketEvents.getAPI().eventManager.registerListener(object : PacketListenerAbstract() {
            override fun onPacketSend(event: PacketSendEvent) {
                if (event.player !is Player) return
                when (event.packetType) {
                    PacketType.Play.Server.SPAWN_ENTITY -> {
                        val packet = WrapperPlayServerSpawnEntity(event)
                        SpigotReflectionUtil.getEntityById(packet.entityId)?.let { mob ->
                            updateEntity(mob) { entity ->
                                engine.call(setOf(entity), ByEntityEvent.Shown(event))
                            }
                        }
                    }
                    PacketType.Play.Server.DESTROY_ENTITIES -> {
                        val packet = WrapperPlayServerDestroyEntities(event)
                        packet.entityIds.forEach { id ->
                            SpigotReflectionUtil.getEntityById(id)?.let { mob ->
                                updateEntity(mob) { entity ->
                                    engine.call(setOf(entity), ByEntityEvent.Hidden(event))
                                }
                            }
                        }
                    }
                }
            }
        })
    }

    override fun loadInternal(log: LogList, settings: ConfigurationNode): Boolean {
        if (super.loadInternal(log, settings)) {
            try {
                this.settings = settings.get { Settings() }
            } catch (ex: SerializationException) {
                log.line(LogLevel.Error, ex) { "Could not load settings file" }
                return false
            }

            try {
                entityResolver.load(settings)
            } catch (ex: Exception) {
                log.line(LogLevel.Error, ex) { "Could not load entity resolver settings" }
                return false
            }

            _itemBlueprints.clear()
            hostableByItem.clearConfigs()
            walkFile(dataFolder.resolve(BLUEPRINT).toPath(),
                onVisit = { path, _  ->
                    if (path.isRegularFile()) {
                        try {
                            val node = AlexandriaAPI.configLoader().path(path).build().load()
                            hostableByItem.load(log, node)

                            node.node(ITEMS).childrenMap().forEach { (_, child) ->
                                _itemBlueprints.register(child.force())
                            }
                            node.node(ENTITIES).childrenMap().forEach { (_, child) ->
                                _entityBlueprints.register(child.force())
                            }
                        } catch (ex: ConfigurateException) {
                            log.line(LogLevel.Warning, ex) { "Could not load data from $path" }
                        }
                    }
                    FileVisitResult.CONTINUE
                },
                onFail = { path, ex ->
                    log.line(LogLevel.Warning, ex) { "Could not load data from $path" }
                    FileVisitResult.CONTINUE
                }
            )

            return true
        }
        return false
    }

    fun componentType(key: Key) = _componentTypes[key.asString()]

    fun registerComponentType(type: SokolComponentType) {
        _componentTypes[type.key.asString()] = type
    }

    fun updateEntity(mob: Entity, callback: (SokolEntity) -> Unit) {
        persistence.getTag(mob.persistentDataContainer, persistence.entityKey)?.let { tag ->
            val entity = persistence.readEntity(tag)
            entity.add(hostedByEntity(mob))
            callback(entity)
            persistence.writeEntity(entity, persistence.forceTag(mob.persistentDataContainer, persistence.entityKey))
        }
    }
}
