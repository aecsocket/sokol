package com.gitlab.aecsocket.sokol.paper

import com.github.retrooper.packetevents.PacketEvents
import com.gitlab.aecsocket.alexandria.core.LogLevel
import com.gitlab.aecsocket.alexandria.core.LogList
import com.gitlab.aecsocket.alexandria.core.extension.*
import com.gitlab.aecsocket.alexandria.core.keyed.*
import com.gitlab.aecsocket.alexandria.paper.*
import com.gitlab.aecsocket.alexandria.paper.extension.*
import com.gitlab.aecsocket.craftbullet.core.Timings
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.component.*
import net.kyori.adventure.key.Key
import org.bstats.bukkit.Metrics
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.serialize.SerializationException

internal typealias Mob = Entity
internal typealias Item = ItemStack

private const val CONFIG = "config"
private const val ENTITY_PROFILES = "entity_profiles"

private const val BSTATS_ID = 11870

private lateinit var instance: Sokol
val SokolAPI get() = instance

fun interface SokolInputHandler {
    fun handle(event: PlayerInputEvent)
}

class Sokol : BasePlugin(), SokolAPI {
    @ConfigSerializable
    data class Settings(
        val enableBstats: Boolean = true,
        val entityHoverDistance: Float = 0f,
    )

    private data class Registration(
        val onInit: InitContext.() -> Unit,
        val onPostInit: PostInitContext.() -> Unit,
    )

    interface InitContext {
        val engine: SokolEngine.Builder

        fun registerComponentType(type: ComponentType)
    }

    interface PostInitContext

    init {
        instance = this
    }

    private lateinit var command: SokolCommand
    lateinit var settings: Settings private set
    override lateinit var engine: SokolEngine private set
    lateinit var space: SokolSpace private set

    private val _componentTypes = HashMap<String, ComponentType>()
    val componentTypes: Map<String, ComponentType> get() = _componentTypes

    private val _entityProfiles = Registry.create<KeyedEntityProfile>()
    val entityProfiles: Registry<KeyedEntityProfile> get() = _entityProfiles

    override val persistence = PaperSokolPersistence(this)
    val resolver = EntityResolver(this)
    val hoster = EntityHoster(this)
    val timings = Timings(60 * 1000)

    internal val mobsAdded = HashSet<Int>()
    private val registrations = ArrayList<Registration>()
    private val inputHandlers = ArrayList<SokolInputHandler>()
    private var hasReloaded = false

    override fun onEnable() {
        super.onEnable()
        command = SokolCommand(this)
        AlexandriaAPI.registerConsumer(this,
            onInit = {
                serializers
                    .registerExact(ComponentProfileSerializer(this@Sokol))
                    .registerExact(EntityProfileSerializer)
                    .registerExact(KeyedEntityProfileSerializer)
                    .registerExact(MeshesStatic.MeshDefinitionSerializer)
                    .register(EntitySerializer(this@Sokol))
                    .register(DeltaSerializer)
            },
            onLoad = {
                addDefaultI18N()
            }
        )
        AlexandriaAPI.inputHandler { event ->
            val sokolEvent = PlayerInputEvent(event)
            inputHandlers.forEach { handler ->
                handler.handle(sokolEvent)
            }
        }

        registerDefaultConsumer()

        registerEvents(SokolEventListener(this))
        PacketEvents.getAPI().eventManager.registerListener(SokolPacketListener(this@Sokol))
    }

    override fun initInternal(): Boolean {
        if (super.initInternal()) {
            val engineBuilder = SokolEngine.Builder()
            val initCtx = object : InitContext {
                override val engine get() = engineBuilder

                override fun registerComponentType(type: ComponentType) {
                    val key = type.key.asString()
                    if (_componentTypes.contains(key))
                        throw IllegalArgumentException("Component type with key $key already exists")
                    _componentTypes[key] = type
                }
            }

            try {
                registrations.forEach { it.onInit(initCtx) }
                engine = engineBuilder.build()
            } catch (ex: Exception) {
                log.line(LogLevel.Error, ex) { "Could not set up engine" }
                return false
            }

            command.enable()
            persistence.enable()
            resolver.enable()
            hoster.enable()
            space = engine.emptySpace()

            log.line(LogLevel.Info) { "Set up ${engineBuilder.countComponentTypes()} component types, ${engineBuilder.countSystemFactories()} systems" }

            val postInitCtx = object : PostInitContext {}
            registrations.forEach { it.onPostInit(postInitCtx) }

            scheduleRepeating {
                timings.time {
                    resolver.resolve {
                        it.construct()
                        if (hasReloaded)
                            it.call(ReloadEvent)
                        it.update()
                        it.write()
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

            if (this.settings.enableBstats) {
                Metrics(this, BSTATS_ID)
            }

            _entityProfiles.clear()

            val configs = walkConfigs(dataFolder.resolve(CONFIG),
                onError = { ex, path ->
                    log.line(LogLevel.Warning, ex) { "Could not load config from $path" }
                }
            )

            configs.forEach { (node, path) ->
                try {
                    node.node(ENTITY_PROFILES).childrenMap().forEach { (key, child) ->
                        try {
                            val entityProfile = child.force<KeyedEntityProfile>()
                            _entityProfiles.register(entityProfile)
                        } catch (ex: SerializationException) {
                            log.line(LogLevel.Warning, ex) { "Could not read entity profile '$key' from $path" }
                        }
                    }
                } catch (ex: SerializationException) {
                    log.line(LogLevel.Warning, ex) { "Could not read entity profiles from $path" }
                }
            }

            log.line(LogLevel.Info) { "Loaded ${_entityProfiles.size} entity profiles" }

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

    fun inputHandler(handler: SokolInputHandler) {
        inputHandlers.add(handler)
    }

    override fun entityProfile(id: String) = _entityProfiles[id]

    override fun componentType(key: Key) = _componentTypes[key.asString()]

    fun useSpace(capacity: Int = 64, write: Boolean = true, consumer: (SokolSpace) -> Unit) {
        val space = engine.emptySpace(capacity)
        consumer(space)
        if (write) {
            space.write()
        }
    }

    private fun registerDefaultConsumer() {
        registerConsumer(
            onInit = {
                engine
                    .systemFactory { IsWorldTarget }
                    .systemFactory { IsChunkTarget }
                    .systemFactory { IsMobTarget }
                    .systemFactory { IsBlockTarget }
                    .systemFactory { IsItemFormTarget }
                    .systemFactory { IsItemTarget }
                    .systemFactory { PersistenceSystem(this@Sokol, it) }
                    .systemFactory { BlockPersistSystem(it) }
                    .systemFactory { ItemPersistSystem(this@Sokol, it) }
                    .systemFactory { ItemTagPersistSystem(it) }
                    .systemFactory { CompositeSystem(it) }
                    .systemFactory { PositionTarget }
                    .systemFactory { VelocityTarget }
                    .systemFactory { PlayerTrackedTarget }
                    .systemFactory { MobConstructorSystem(it) }
                    .systemFactory { ParticleEffectSpawnerSystem(it) }
                    .systemFactory { MeshesTarget }
                    .systemFactory { MeshesStaticSystem(it) }
                    .systemFactory { MeshesItemSystem(this@Sokol, it) }
                    .systemFactory { MeshesInWorldSystem(it) }
                    .systemFactory { MeshesInWorldMobSystem(it) }

                    .componentType<Profiled>()
                    .componentType<InTag>()
                    .componentType<IsWorld>()
                    .componentType<IsChunk>()
                    .componentType<IsMob>()
                    .componentType<IsBlock>()
                    .componentType<IsItem>()
                    .componentType<ItemHolder>()
                    .componentType<InItemTag>()
                    .componentType<AsMob>()
                    .componentType<AsItem>()
                    .componentType<IsRoot>()
                    .componentType<IsChild>()
                    .componentType<PositionRead>()
                    .componentType<PositionWrite>()
                    .componentType<VelocityRead>()
                    .componentType<PlayerTracked>()
                    .componentType<Rotation>()
                    .componentType<ParticleEffectSpawner>()
                    .componentType<Meshes>()
                    .componentType<MeshesStatic>()
                    .componentType<MeshesItem>()
                    .componentType<MeshesInWorld>()
                registerComponentType(AsMob.Type)
                registerComponentType(AsItem.Type)
                registerComponentType(Rotation.Type)
                registerComponentType(ParticleEffectSpawner.Type)
                registerComponentType(MeshesStatic.Type)
                registerComponentType(MeshesItem.Type)
                registerComponentType(MeshesInWorld.Type)
            }
        )
    }
}
