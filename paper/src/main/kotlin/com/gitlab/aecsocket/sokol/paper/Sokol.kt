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
import com.gitlab.aecsocket.sokol.core.util.Timings
import com.gitlab.aecsocket.sokol.paper.feature.*
import io.github.retrooper.packetevents.util.SpigotReflectionUtil
import net.kyori.adventure.key.Key
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.serialize.SerializationException
import java.nio.file.FileVisitResult
import kotlin.io.path.isRegularFile

private const val CONFIG = "config"
private const val ITEMS = "items"
private const val ENTITIES = "entities"
internal const val TIMING_MAX_MEASUREMENTS = 60 * TPS

private lateinit var instance: Sokol
val SokolAPI get() = instance

class Sokol : BasePlugin() {
    @ConfigSerializable
    data class Settings(
        val enabled: Boolean = false
    )

    private data class Registration(
        val onInit: InitContext.() -> Unit,
    )

    interface InitContext {
        val engine: SokolEngine.Builder

        fun registerComponentType(type: PersistentComponentType)
    }

    init {
        instance = this
    }

    lateinit var settings: Settings private set
    lateinit var engine: SokolEngine private set

    private val _componentTypes = HashMap<String, PersistentComponentType>()
    val componentTypes: Map<String, PersistentComponentType> get() = _componentTypes

    /*
    private val _itemBlueprints = Registry.create<ItemBlueprint>()
    val itemBlueprints: Registry<ItemBlueprint> get() = _itemBlueprints*/

    private val _entityBlueprints = Registry.create<KeyedEntityBlueprint>()
    val entityBlueprints: Registry<KeyedEntityBlueprint> get() = _entityBlueprints

    val persistence = SokolPersistence(this)
    val entityResolver = EntityResolver(this)

    val resolverTimings = Timings(TIMING_MAX_MEASUREMENTS)
    val engineTimings = Timings(TIMING_MAX_MEASUREMENTS)

    //private val hostableByItem = HostableByItem()

    private val registrations = ArrayList<Registration>()

    override fun onEnable() {
        super.onEnable()
        SokolCommand(this)
        AlexandriaAPI.registerConsumer(this,
            onInit = {
                serializers
                    .register(PersistentComponent::class, ComponentSerializer(this@Sokol))
                    //.registerExact(ItemBlueprint::class, ItemBlueprintSerializer(this@Sokol))
                    .registerExact(SokolBlueprint::class, BlueprintSerializer(this@Sokol))
                    .registerExact(KeyedEntityBlueprint::class, EntityBlueprintSerializer(this@Sokol))
            },
            onLoad = {
                addDefaultI18N()
            }
        )

        registerConsumer {
            engine
                .systemFactory { ColliderSystem(it) }
                .systemFactory { MeshSystem(it) }

                .componentType(NBTTagAccessor)
                .componentType(HostedByWorld)
                .componentType(HostedByChunk)
                .componentType(HostedByEntity)
                .componentType(HostedByBlock)
                .componentType(HostedByItem)

                .componentType(HostableByEntity)
                .componentType(Collider)
                .componentType(Mesh)
            registerComponentType(HostableByEntity.Type())
            registerComponentType(Collider.Type())
            registerComponentType(Mesh.Type())
        }

        registerEvents(object : Listener {
            @EventHandler
            fun EntityAddToWorldEvent.on() {
                val mob = entity
                updateEntity(mob) { space, _ ->
                    space.call(ByEntityEvent.Added(this))
                }
            }

            @EventHandler
            fun EntityRemoveFromWorldEvent.on() {
                val mob = entity
                updateEntity(mob) { space, _ ->
                    space.call(ByEntityEvent.Removed(this))
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
                            updateEntity(mob) { space, _ ->
                                space.call(ByEntityEvent.Shown(event))
                            }
                        }
                    }
                    PacketType.Play.Server.DESTROY_ENTITIES -> {
                        val packet = WrapperPlayServerDestroyEntities(event)
                        val entityIds = packet.entityIds.toMutableList()

                        val iter = entityIds.iterator()
                        while (iter.hasNext()) {
                            val id = iter.next()
                            SpigotReflectionUtil.getEntityById(id)?.let { mob ->
                                updateEntity(mob) { space, _ ->
                                    if (space.call(ByEntityEvent.Hidden(event)).thisEntityCancelled) {
                                        iter.remove()
                                    }
                                }
                            }
                        }

                        packet.entityIds = entityIds.toIntArray()
                    }
                }
            }
        })
    }

    override fun init() {
        val engineBuilder = SokolEngine.Builder()
        val initCtx = object : InitContext {
            override val engine get() = engineBuilder

            override fun registerComponentType(type: PersistentComponentType) {
                _componentTypes[type.key.asString()] = type
            }
        }
        registrations.forEach { it.onInit(initCtx) }
        engine = engineBuilder.build()

        log.line(LogLevel.Info) { "Set up ${engineBuilder.componentTypes.size} transient component types, ${_componentTypes.size} persistent component types, ${engine.systems.size} systems" }

        scheduleRepeating {
            val space = engine.createSpace()
            resolverTimings.time { entityResolver.resolve(space) }
            engineTimings.time { space.call(UpdateEvent) }
        }

        super.init()
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

            //_itemBlueprints.clear()
            //hostableByItem.clearConfigs()
            _entityBlueprints.clear()
            val configDir = dataFolder.resolve(CONFIG)
            if (configDir.exists()) {
                walkFile(configDir.toPath(),
                    onVisit = { path, _ ->
                        if (path.isRegularFile()) {
                            try {
                                val node = AlexandriaAPI.configLoader().path(path).build().load()
                                //hostableByItem.load(log, node)

                                /*node.node(ITEMS).childrenMap().forEach { (_, child) ->
                                _itemBlueprints.register(child.force())
                            }*/
                                node.node(ENTITIES).childrenMap().forEach { (_, child) ->
                                    _entityBlueprints.register(child.force())
                                }
                            } catch (ex: Exception) {
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
            }
            log.line(LogLevel.Info) { "Loaded ${_entityBlueprints.size} entity BPs" }

            return true
        }
        return false
    }

    fun registerConsumer(
        onInit: InitContext.() -> Unit = {}
    ) {
        registrations.add(Registration(onInit))
    }

    fun componentType(key: Key) = _componentTypes[key.asString()]

    fun updateEntity(mob: Entity, callback: (SokolEngine.Space, Int) -> Unit) {
        persistence.getTag(mob.persistentDataContainer, persistence.entityKey)?.let { tag ->
            val space = engine.createSpace(1)
            val entity = persistence.readBlueprint(tag).create(space)
            space.addComponent(entity, NBTTagAccessor(tag))
            space.addComponent(entity, hostedByEntity(mob))
            callback(space, entity)
        }
    }
}
