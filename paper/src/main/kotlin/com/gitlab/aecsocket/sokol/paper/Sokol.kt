package com.gitlab.aecsocket.sokol.paper

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.gitlab.aecsocket.alexandria.core.LogLevel
import com.gitlab.aecsocket.alexandria.core.LogList
import com.gitlab.aecsocket.alexandria.core.extension.*
import com.gitlab.aecsocket.alexandria.core.input.Input
import com.gitlab.aecsocket.alexandria.core.keyed.Registry
import com.gitlab.aecsocket.alexandria.core.physics.Quaternion
import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.AlexandriaAPI
import com.gitlab.aecsocket.alexandria.paper.BasePlugin
import com.gitlab.aecsocket.alexandria.paper.PacketInputListener
import com.gitlab.aecsocket.alexandria.paper.extension.location
import com.gitlab.aecsocket.alexandria.paper.extension.position
import com.gitlab.aecsocket.alexandria.paper.extension.registerEvents
import com.gitlab.aecsocket.alexandria.paper.extension.scheduleRepeating
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.util.Timings
import com.gitlab.aecsocket.sokol.paper.component.*
import net.kyori.adventure.key.Key
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataContainer
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.serialize.SerializationException
import java.nio.file.FileVisitResult
import java.nio.file.Path
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
        val onPostInit: PostInitContext.() -> Unit,
    )

    interface InitContext {
        val engine: SokolEngine.Builder

        fun registerComponentType(type: PersistentComponentType)
    }

    interface PostInitContext

    init {
        instance = this
    }

    lateinit var settings: Settings private set
    lateinit var engine: SokolEngine private set

    private val _componentTypes = HashMap<String, PersistentComponentType>()
    val componentTypes: Map<String, PersistentComponentType> get() = _componentTypes

    private val _itemBlueprints = Registry.create<KeyedItemBlueprint>()
    val itemBlueprints: Registry<KeyedItemBlueprint> get() = _itemBlueprints

    private val _entityBlueprints = Registry.create<KeyedEntityBlueprint>()
    val entityBlueprints: Registry<KeyedEntityBlueprint> get() = _entityBlueprints

    val persistence = SokolPersistence(this)
    val entityResolver = EntityResolver(this)

    val engineTimings = Timings(TIMING_MAX_MEASUREMENTS)

    val hostableByItem = HostableByItem.Type()
    val colliders = Collider.Type()
    val staticMeshes = StaticMesh.Type()

    internal val entitiesAdded = HashSet<Int>()
    private val registrations = ArrayList<Registration>()
    private var hasReloaded = false

    override fun onEnable() {
        super.onEnable()
        SokolCommand(this)
        AlexandriaAPI.registerConsumer(this,
            onInit = {
                serializers
                    .registerExact(PersistentComponent::class, ComponentSerializer(this@Sokol))
                    .registerExact(PersistentComponentFactory::class, ComponentFactorySerializer(this@Sokol))
                    .registerExact(KeyedItemBlueprint::class, ItemBlueprintSerializer(this@Sokol))
                    .registerExact(KeyedEntityBlueprint::class, EntityBlueprintSerializer(this@Sokol))
                    .registerExact(Mesh.PartDefinition::class, Mesh.PartDefinitionSerializer)
                    .registerExact(StaticMesh.Config::class, StaticMesh.ConfigSerializer)
            },
            onLoad = {
                addDefaultI18N()
            }
        )

        registerDefaultConsumer()

        registerEvents(SokolEventListener(this))
        PacketEvents.getAPI().eventManager.apply {
            registerListener(SokolPacketListener(this@Sokol))
            registerListener(PacketInputListener(::onInput), PacketListenerPriority.NORMAL)
        }
    }

    override fun initInternal(): Boolean {
        if (super.initInternal()) {
            val engineBuilder = SokolEngine.Builder()
            val initCtx = object : InitContext {
                override val engine get() = engineBuilder

                override fun registerComponentType(type: PersistentComponentType) {
                    _componentTypes[type.key.asString()] = type
                }
            }
            try {
                registrations.forEach { it.onInit(initCtx) }
                engine = engineBuilder.build()
            } catch (ex: Exception) {
                log.line(LogLevel.Error, ex) { "Could not set up engine" }
                return false
            }

            log.line(LogLevel.Info) { "Set up ${engineBuilder.componentTypes.size} transient component types, ${_componentTypes.size} persistent component types, ${engine.systems.size} systems" }

            val postInitCtx = object : PostInitContext {}
            registrations.forEach { it.onPostInit(postInitCtx) }

            scheduleRepeating {
                engineTimings.time {
                    entityResolver.resolve {
                        if (hasReloaded)
                            it.call(SokolEvent.Reload)
                        it.call(SokolEvent.Update)
                    }
                    hasReloaded = false
                }
            }

            return true
        }
        return false
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
            _entityBlueprints.clear()
            hostableByItem.registry.clear()
            colliders.registry.clear()
            staticMeshes.registry.clear()
            val configDir = dataFolder.resolve(CONFIG)
            if (configDir.exists()) {
                data class FileData(
                    val node: ConfigurationNode,
                    val path: Path
                )

                val nodes = ArrayList<FileData>()
                walkFile(configDir.toPath(),
                    onVisit = { path, _ ->
                        if (path.isRegularFile()) {
                            try {
                                val node = AlexandriaAPI.configLoader().path(path).build().load()
                                nodes.add(FileData(node, path))
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

                fun loadNodes(action: (ConfigurationNode) -> Unit) {
                    nodes.forEach { (node, path) ->
                        try {
                            action(node)
                        } catch (ex: Exception) {
                            log.line(LogLevel.Warning, ex) { "Could not deserialize data from $path" }
                        }
                    }
                }

                loadNodes { hostableByItem.load(it) }
                loadNodes { colliders.load(it) }
                loadNodes { staticMeshes.load(it) }
                loadNodes {
                    it.node(ITEMS).childrenMap().forEach { (_, child) ->
                        _itemBlueprints.register(child.force())
                    }
                }
                loadNodes {
                    it.node(ENTITIES).childrenMap().forEach { (_, child) ->
                        _entityBlueprints.register(child.force())
                    }
                }
            }
            log.line(LogLevel.Info) { "Loaded ${_entityBlueprints.size} entity blueprints, ${_itemBlueprints.size} item blueprints" }

            hasReloaded = true
            return true
        }
        return false
    }

    fun registerConsumer(
        onInit: InitContext.() -> Unit = {},
        onPostInit: PostInitContext.() -> Unit = {},
    ) {
        registrations.add(Registration(onInit, onPostInit))
    }

    fun componentType(key: Key) = _componentTypes[key.asString()]

    fun usePDC(pdc: PersistentDataContainer, write: Boolean = true, callback: (SokolEntityAccess) -> Unit): Boolean {
        persistence.getTag(pdc, persistence.entityKey)?.let { tag ->
            val blueprint = persistence.readBlueprint(tag)
            if (blueprint.isNotEmpty()) {
                val entity = blueprint.create(engine)
                callback(entity)
                if (write) {
                    // TODO high priority: only reserialize components into the tag if it's actually changed (been dirtied)
                    // how to implement this? idfk
                    persistence.writeEntity(entity, tag)
                }
            }
            return true
        }
        return false
    }

    fun useMob(mob: Entity, write: Boolean = true, callback: (SokolEntityAccess) -> Unit): Boolean {
        return usePDC(mob.persistentDataContainer, write) { entity ->
            entityResolver.populate(entity, mob)
            callback(entity)
        }
    }

    fun useItem(stack: ItemStack, write: Boolean = true, callback: (SokolEntityAccess) -> Unit): Boolean {
        val meta = stack.itemMeta
        return usePDC(meta.persistentDataContainer, write) { entity ->
            entityResolver.populate(entity, stack, meta)
            callback(entity)
        }
    }

    fun usePlayerItems(player: Player, write: Boolean = true, callback: (SokolEntityAccess) -> Unit) {
        player.inventory.forEachIndexed { idx, stack ->
            stack?.let {
                useItem(stack, write) { entity ->
                    entity.addComponent(itemHolderOfPlayer(player, idx))
                    callback(entity)
                }
            }
        }
    }

    private fun onInput(event: PacketInputListener.Event) {
        val player = event.player
        when (val input = event.input) {
            is Input.Drop -> return
            else -> {
                usePlayerItems(player, false) { entity ->
                    entity.call(PlayerInput(input, player) { event.cancel() })
                }
            }
        }
    }

    private fun registerDefaultConsumer() {
        registerConsumer(
            onInit = {
                engine
                    .systemFactory { it.define(ColliderSystem(it)) }
                    .systemFactory { it.define(MeshSystem(it)) }
                    .systemFactory { it.define(StaticMeshSystem(it)) }

                    .componentType<HostedByWorld>()
                    .componentType<HostedByChunk>()
                    .componentType<HostedByMob>()
                    .componentType<HostedByBlock>()
                    .componentType<HostedByItem>()
                    .componentType<Position>()
                    .componentType<IsValidSupplier>()

                    .componentType<HostableByItem>()
                    .componentType<HostableByEntity>()
                    .componentType<Rotation>()
                    .componentType<Collider>()
                    .componentType<RigidBody>()
                    .componentType<VehicleBody>()
                    .componentType<Mesh>()
                    .componentType<StaticMesh>()
                registerComponentType(hostableByItem)
                registerComponentType(HostableByEntity.Type)
                registerComponentType(Rotation.Type)
                registerComponentType(colliders)
                registerComponentType(RigidBody.Type)
                registerComponentType(VehicleBody.Type)
                registerComponentType(Mesh.Type)
                registerComponentType(staticMeshes)
            },
            onPostInit = {
                val mRotation = engine.componentMapper<Rotation>()

                entityResolver.mobPopulator { entity, mob ->
                    entity.addComponent(object : HostedByMob {
                        override val mob get() = mob
                    })
                    entity.addComponent(object : IsValidSupplier {
                        override val valid: () -> Boolean get() = { mob.isValid }
                    })

                    val rotation = mRotation.mapOr(entity)
                    var transform = Transform(mob.location.position(), rotation?.rotation ?: Quaternion.Identity)
                    entity.addComponent(object : Position {
                        override val world get() = mob.world
                        @Suppress("UnstableApiUsage")
                        override var transform: Transform
                            get() = transform
                            set(value) {
                                transform = value
                                rotation?.rotation = value.rotation
                                mob.teleport(value.translation.location(world), true)
                            }
                    })
                }
            }
        )
    }
}
