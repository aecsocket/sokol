package com.gitlab.aecsocket.sokol.paper

import com.github.retrooper.packetevents.PacketEvents
import com.gitlab.aecsocket.alexandria.core.LogLevel
import com.gitlab.aecsocket.alexandria.core.LogList
import com.gitlab.aecsocket.alexandria.core.extension.*
import com.gitlab.aecsocket.alexandria.core.keyed.*
import com.gitlab.aecsocket.alexandria.paper.*
import com.gitlab.aecsocket.alexandria.paper.effect.ParticleEffect
import com.gitlab.aecsocket.alexandria.paper.extension.*
import com.gitlab.aecsocket.craftbullet.core.Timings
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.component.*
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.format.TextColor
import org.bstats.bukkit.Metrics
import org.bukkit.Color
import org.bukkit.Particle
import org.bukkit.Particle.DustOptions
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.serialize.SerializationException
import kotlin.reflect.KClass

internal typealias Mob = Entity
internal typealias Item = ItemStack

private const val CONFIG = "config"
private const val ENTITY_PROFILES = "entity_profiles"

private const val BSTATS_ID = 11870
const val REPLACE_MARKER = "%%"

private lateinit var instance: Sokol
val SokolAPI get() = instance

fun interface InputListener {
    fun run(event: PlayerInputEvent)
}

class Sokol : BasePlugin(PluginManifest("sokol",
    accentColor = TextColor.color(0x329945),
    langPaths = listOf(
        "lang/default_en-US.conf"
    ),
    savedPaths = listOf(
        "settings.conf"
    )
)), SokolAPI {
    @ConfigSerializable
    data class Settings(
        val enableBstats: Boolean = true,
        val resolveContainerBlocks: Boolean = true,
        val resolveContainerItems: Boolean = true,
        val entityHoverDistance: Double = 4.0,
        val debugDraw: DebugDraw = DebugDraw(),
    )

    @ConfigSerializable
    data class DebugDraw(
        val radius: Double = 16.0,
        val hoverShape: ParticleEngineEffect = ParticleEngineEffect(listOf(
            ParticleEffect(Particle.BLOCK_DUST, data = DustOptions(Color.RED, 0.5f))
        )),
        val slots: ParticleEngineEffect = ParticleEngineEffect(listOf(
            ParticleEffect(Particle.BLOCK_DUST, data = DustOptions(Color.GREEN, 0.5f))
        )),
    )

    private data class Registration(
        val onInit: InitContext.() -> Unit,
        val onPostInit: PostInitContext.() -> Unit,
    )

    interface InitContext {
        val sokol: Sokol
        val components: SokolComponents

        fun <C : SokolComponent> componentType(type: ComponentType<C>)
        fun <C : SokolComponent> componentClass(type: KClass<C>)
        fun system(factory: EngineSystemFactory)
    }

    interface PostInitContext

    init {
        instance = this
    }

    private lateinit var command: SokolCommand
    lateinit var settings: Settings private set
    override lateinit var engine: SokolEngine private set

    private val _componentTypes = HashMap<String, ComponentType<*>>()
    val componentTypes: Map<String, ComponentType<*>> get() = _componentTypes

    private val _entityProfiles = Registry.create<KeyedEntityProfile>()
    val entityProfiles: Registry<KeyedEntityProfile> get() = _entityProfiles

    override val persistence = PaperSokolPersistence(this)
    val resolver = EntityResolver(this)
    val hoster = EntityHoster(this)
    val timings = Timings(60 * 1000)
    val holding = EntityHolding(this)
    val players = SokolPlayers(this)
    val physics = EntityPhysics(this)
    val components = SokolComponents()
    lateinit var space: EntityCollection private set

    internal val mobsAdded = HashSet<Int>()
    private val registrations = ArrayList<Registration>()
    private val onInput = ArrayList<InputListener>()
    internal var hasReloaded = false

    override fun onEnable() {
        super.onEnable()
        command = SokolCommand(this)
        AlexandriaAPI.registerConsumer(this,
            onInit = { ctx ->
                ctx.serializers
                    .register(matchExactErased<ComponentProfile<*>>(), ComponentProfileSerializer(this@Sokol))
                    .registerExact(EntityProfileSerializer)
                    .registerExact(KeyedEntityProfileSerializer)
                    .registerExact(EntityRuleSerializer(components.rules))
                    .registerExact(EntityCallbackSerializer(components.callbacks))
                    .registerExact(LoreProviderSerializer(components.itemLoreManager))
                    .registerExact(MeshProviderStatic.MeshDefinitionSerializer)
                    .registerExact(EntitySerializer(this@Sokol))
                    .registerExact(BlueprintSerializer(this@Sokol))
                    .register(DeltaSerializer)
                    .register(matchExactErased<Stat<*>>(), StatSerializer(components.stats))
                    .register(matchExactErased<StatFormatter<*>>(), StatFormatterSerializer(components.itemLoreStats))
            },
            onLoad = { ctx ->
                ctx.addDefaultI18N()
            }
        )
        AlexandriaAPI.onInput { event ->
            val sokolEvent = PlayerInputEvent(event)
            onInput.forEach { it.run(sokolEvent) }
        }

        registerDefaultConsumer()

        registerEvents(SokolEventListener(this))
        PacketEvents.getAPI().eventManager.registerListener(SokolPacketListener(this))
    }

    override fun initInternal(): Boolean {
        if (super.initInternal()) {
            val engineBuilder = SokolEngine.Builder()
            val initCtx = object : InitContext {
                override val sokol get() = this@Sokol
                override val components get() = this@Sokol.components

                override fun <C : SokolComponent> componentType(type: ComponentType<C>) {
                    val key = type.key.asString()
                    if (_componentTypes.contains(key))
                        throw IllegalArgumentException("Persistent component type with key $key already exists")
                    _componentTypes[key] = type
                }

                override fun <C : SokolComponent> componentClass(type: KClass<C>) {
                    engineBuilder.componentClass(type)
                }

                override fun system(factory: EngineSystemFactory) {
                    engineBuilder.systemFactory(factory)
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
            holding.enable()
            players.enable()
            physics.enable()

            log.line(LogLevel.Info) { "Set up ${engineBuilder.countComponentTypes()} component types, ${engineBuilder.countSystemFactories()} systems" }

            val postInitCtx = object : PostInitContext {}
            registrations.forEach { it.onPostInit(postInitCtx) }

            return true
        }
        return false
    }

    override fun loadInternal(log: LogList, config: ConfigurationNode): Boolean {
        if (!super.loadInternal(log, config)) return false
        settings = config.force()

        if (settings.enableBstats) {
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

    override fun onDisable() {
        resolver.disable()
    }

    fun registerConsumer(
        onInit: (InitContext) -> Unit = {},
        onPostInit: (PostInitContext) -> Unit = {},
    ) {
        registrations.add(Registration(onInit, onPostInit))
    }

    fun onInput(handler: InputListener) {
        onInput.add(handler)
    }

    override fun entityProfile(id: String) = _entityProfiles[id]

    override fun componentType(key: Key) = _componentTypes[key.asString()]

    fun useSpace(write: Boolean = true, capacity: Int = 64, consumer: (EntityCollection) -> Unit) {
        val space = engine.emptySpace(capacity)
        consumer(space)
        if (write) {
            space.write()
        }
    }

    fun useSpaceOf(entities: Iterable<SokolEntity>, write: Boolean = true, consumer: (EntitySpace) -> Unit) {
        val space = engine.spaceOf(entities)
        consumer(space)
        if (write) {
            space.write()
        }
    }

    fun useSpaceOf(entity: SokolEntity, write: Boolean = true, consumer: (EntitySpace) -> Unit) {
        val space = engine.spaceOf(entity)
        consumer(space)
        if (write) {
            space.write()
        }
    }

    private fun registerDefaultConsumer() {
        registerConsumer(
            onInit = { ctx ->
                ctx.transientComponent<Profiled>()
                ctx.transientComponent<Composite>()
                ctx.transientComponent<InTag>()
                ctx.transientComponent<IsChild>()
                ctx.system { PersistenceSystem(this, it) }

                DisplayName.init(ctx)
                DisplayNameFromProfile.init(ctx)
                Tagged.init(ctx)

                Hosted.init(ctx)
                PositionAccess.init(ctx)
                VelocityAccess.init(ctx)
                ItemHolder.init(ctx)
                Removable.init(ctx)
                RemovableFromParent.init(ctx)
                PlayerTracked.init(ctx)
                PlayerTrackedFromParent.init(ctx)

                EntitySlot.init(ctx)
                EntitySlotInMap.init(ctx)
                ContainerMap.init(ctx)
                Stats.init(ctx)
                Hostable.init(ctx)
                Rotation.init(ctx)

                DeltaTransform.init(ctx)
                DeltaTransformStatic.init(ctx)
                PositionFromParent.init(ctx)

                InputCallbacks.init(ctx)
                InputRemovable.init(ctx)
                PlaceableAsMob.init(ctx)
                TakeableAsItem.init(ctx)

                ItemDisplayName.init(ctx)
                ItemLoreManager.init(ctx)
                ItemLoreStatic.init(ctx)
                ItemLoreFromProfile.init(ctx)
                ItemLoreStats.init(ctx)
                ItemLoreContainerMap.init(ctx)

                PositionEffects.init(ctx)
                MeshProvider.init(ctx)
                MeshProviderStatic.init(ctx)
                MeshProviderFromItem.init(ctx)
                MeshesInWorld.init(ctx)

                HoverShape.init(ctx)
                HoverMeshGlow.init(ctx)
                Holdable.init(ctx)
                Held.init(ctx)
                HeldMeshGlow.init(ctx)
                HeldSnap.init(ctx)
                HeldAttachable.init(ctx)
                HeldCompositeEffects.init(ctx)
                HoldMovable.init(ctx)
                HoldDetachable.init(ctx)

                Collider.init(ctx)
                ColliderEffects.init(ctx)
            }
        )
    }
}

inline fun <reified C : SokolComponent> Sokol.InitContext.persistentComponent(type: ComponentType<C>) {
    componentType(type)
    transientComponent<C>()
}

inline fun <reified C : SokolComponent> Sokol.InitContext.transientComponent() = componentClass(C::class)
