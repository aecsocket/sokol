package com.gitlab.aecsocket.sokol.paper

import com.github.retrooper.packetevents.PacketEvents
import com.gitlab.aecsocket.alexandria.core.LogLevel
import com.gitlab.aecsocket.alexandria.core.LogList
import com.gitlab.aecsocket.alexandria.core.extension.*
import com.gitlab.aecsocket.alexandria.core.keyed.*
import com.gitlab.aecsocket.alexandria.core.physics.Vector3
import com.gitlab.aecsocket.alexandria.paper.*
import com.gitlab.aecsocket.alexandria.paper.effect.ParticleEffect
import com.gitlab.aecsocket.alexandria.paper.extension.*
import com.gitlab.aecsocket.craftbullet.core.ContactManifoldPoint
import com.gitlab.aecsocket.craftbullet.core.ServerPhysicsSpace
import com.gitlab.aecsocket.craftbullet.core.Timings
import com.gitlab.aecsocket.craftbullet.core.physPosition
import com.gitlab.aecsocket.craftbullet.paper.CraftBulletAPI
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.extension.bullet
import com.gitlab.aecsocket.sokol.paper.component.*
import com.gitlab.aecsocket.sokol.paper.stat.NumberStatBarFormatter
import com.gitlab.aecsocket.sokol.paper.stat.NumberStatFormatter
import com.gitlab.aecsocket.sokol.paper.stat.NameStatFormatter
import com.jme3.bullet.collision.PhysicsCollisionObject
import com.jme3.bullet.collision.shapes.BoxCollisionShape
import com.jme3.bullet.objects.PhysicsGhostObject
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

private lateinit var instance: Sokol
val SokolAPI get() = instance

typealias InputListener = (event: PlayerInputEvent) -> Unit

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
    val components = SokolComponents(this)
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
                    .registerExact(MeshProviderStatic.MeshDefinitionSerializer)
                    .registerExact(EntitySerializer(this@Sokol))
                    .registerExact(BlueprintSerializer(this@Sokol))
                    .register(DeltaSerializer)
                    .register(matchExactErased<Stat<*>>(), StatSerializer(this@Sokol))
                    .register(matchExactErased<StatFormatter<*>>(), StatFormatterSerializer(this@Sokol))
            },
            onLoad = { ctx ->
                ctx.addDefaultI18N()
            }
        )
        AlexandriaAPI.onInput { event ->
            val sokolEvent = PlayerInputEvent(event)
            onInput.forEach { it(sokolEvent) }
        }

        registerDefaultConsumer()

        registerEvents(SokolEventListener(this))
        PacketEvents.getAPI().eventManager.registerListener(SokolPacketListener(this))

        CraftBulletAPI.onContact(::onPhysicsContact)
        CraftBulletAPI.onPostStep(::onPhysicsPostStep)
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
                ctx.system { IsWorldTarget }
                ctx.system { IsChunkTarget }
                ctx.system { IsMobTarget }
                ctx.system { IsBlockTarget }
                ctx.system { IsItemFormTarget }
                ctx.system { IsItemTarget }
                ctx.system { PersistenceSystem(this, it) }
                ctx.system { BlockPersistSystem(it) }
                ctx.system { ItemPersistSystem(this, it) }
                ctx.system { ItemTagPersistSystem(it) }
                ctx.system { DisplayNameTarget }
                ctx.system { DisplayNameFromProfileSystem(it) }
                ctx.system { MobConstructorSystem(this, it) }
                ctx.system { MobSystem(it) }
                ctx.system { DeltaTransformTarget }
                ctx.system { PositionAccessTarget }
                ctx.system { VelocityAccessTarget }
                ctx.system { PlayerTrackedTarget }
                ctx.system { PlayerTrackedSystem(it) }
                ctx.system { RemovablePreTarget }
                ctx.system { RemovableTarget }
                ctx.system { RemovableSystem(it) }
                ctx.system { PositionEffectsSystem(it) }
                ctx.system { InputCallbacksInstanceTarget }
                ctx.system { InputCallbacksSystem(it) }
                ctx.system { InputCallbacksInstanceSystem(it) }
                ctx.system { InputRemovableSystem(it) }
                ctx.system { TakeableAsItemSystem(this, it) }
                ctx.system { MeshProviderTarget }
                ctx.system { MeshProviderStaticSystem(it) }
                ctx.system { MeshProviderFromItemSystem(this, it) }
                ctx.system { MeshesInWorldInstanceTarget }
                ctx.system { MeshesInWorldSystem(it) }
                ctx.system { MeshesInWorldInstanceSystem(it) }
                ctx.system { MeshesInWorldMobSystem(this, it) }
                ctx.system { HoverMeshGlowSystem(it) }
                ctx.system { ColliderInstanceTarget }
                ctx.system { ColliderSystem(it) }
                ctx.system { ColliderInstanceSystem(it) }
                ctx.system { ColliderInstanceParentSystem(it) }
                ctx.system { ColliderMobSystem(this, it) }
                ctx.system { ColliderEffectsSystem(it) }
                ctx.system { HoldableInputsSystem(this, it) }
                ctx.system { PlaceableAsMobSystem(this, it) }
                ctx.system { HeldColliderSystem(it) }
                ctx.system { HeldMobSystem(this, it) }
                ctx.system { HoldMovableCallbackSystem(this, it) }
                ctx.system { HoldMovableColliderSystem(it) }
                ctx.system { HoldDetachableCallbackSystem(this, it) }
                ctx.system { HoldDetachableColliderSystem(it) }
                ctx.system { HeldSnapSystem(it) }
                ctx.system { HeldAttachableEffectsSystem(it) }
                ctx.system { HeldMeshGlowSystem(it) }
                ctx.system { EntitySlotTarget }
                ctx.system { StatsInstanceTarget }
                ctx.system { StatsSystem(it) }
                ctx.system { ItemDisplayNameSystem(it) }
                ctx.system { ItemLoreManagerSystem(it) }
                ctx.system { ItemLoreStaticSystem(it) }
                ctx.system { ItemLoreFromProfileSystem(it) }
                ctx.system { ItemLoreStatsSystem(it) }

                ctx.transientComponent<Profiled>()
                ctx.transientComponent<Composite>()
                ctx.transientComponent<InTag>()
                ctx.transientComponent<IsWorld>()
                ctx.transientComponent<IsChunk>()
                ctx.transientComponent<IsMob>()
                ctx.transientComponent<IsBlock>()
                ctx.transientComponent<IsItem>()
                ctx.transientComponent<ItemHolder>()
                ctx.transientComponent<InItemTag>()
                ctx.transientComponent<DisplayName>()
                ctx.persistentComponent(DisplayNameFromProfile.Type)
                ctx.persistentComponent(AsMob.Type)
                ctx.persistentComponent(AsItem.Type)
                ctx.transientComponent<IsChild>()
                ctx.persistentComponent(components.containerMap)
                ctx.transientComponent<DeltaTransform>()
                ctx.persistentComponent(Rotation.Type)
                ctx.transientComponent<PositionAccess>()
                ctx.transientComponent<VelocityRead>()
                ctx.transientComponent<PlayerTracked>()
                ctx.transientComponent<Removable>()
                ctx.persistentComponent(InputCallbacks.Type)
                ctx.transientComponent<InputCallbacksInstance>()
                ctx.persistentComponent(InputRemovable.Type)
                ctx.persistentComponent(TakeableAsItem.Type)
                ctx.persistentComponent(PositionEffects.Type)
                ctx.transientComponent<MeshProvider>()
                ctx.persistentComponent(MeshProviderStatic.Type)
                ctx.persistentComponent(MeshProviderFromItem.Type)
                ctx.persistentComponent(MeshesInWorld.Type)
                ctx.transientComponent<MeshesInWorldInstance>()
                ctx.persistentComponent(HoverShape.Type)
                ctx.persistentComponent(HoverMeshGlow.Type)
                ctx.persistentComponent(Collider.Type)
                ctx.transientComponent<ColliderInstance>()
                ctx.persistentComponent(ColliderRigidBody.Type)
                ctx.persistentComponent(ColliderVehicleBody.Type)
                ctx.persistentComponent(ColliderEffects.Type)
                ctx.persistentComponent(Holdable.Type)
                ctx.transientComponent<Held>()
                ctx.persistentComponent(PlaceableAsMob.Type)
                ctx.persistentComponent(HoldMovable.Type)
                ctx.persistentComponent(HoldDetachable.Type)
                ctx.persistentComponent(HeldSnap.Type)
                ctx.persistentComponent(HeldAttachableEffects.Type)
                ctx.persistentComponent(HeldMeshGlow.Type)
                ctx.transientComponent<EntitySlot>()
                ctx.persistentComponent(components.stats)
                ctx.transientComponent<StatsInstance>()
                ctx.persistentComponent(ItemDisplayName.Type)
                ctx.persistentComponent(ItemLoreManager.Type)
                ctx.persistentComponent(ItemLoreStatic.Type)
                ctx.persistentComponent(ItemLoreFromProfile.Type)
                ctx.persistentComponent(components.itemLoreStats)

                DeltaTransformStatic.register(ctx)
                PositionFromDelta.register(ctx)
                EntitySlotInMap.register(ctx)
                HeldAttachable.register(ctx)
                // TODO move to own func
                // move all of this to their own funcs really
                components.stats.apply {
                    stats(ColliderRigidBody.Stats.All)
                }

                components.itemLoreStats.apply {
                    formatterType<NameStatFormatter>(key("name"))
                    formatterType<NumberStatFormatter>(key("number"))
                    formatterType<NumberStatBarFormatter>(key("number_bar"))
                }
            }
        )
    }

    private fun onPhysicsContact(
        space: ServerPhysicsSpace,
        bodyA: PhysicsCollisionObject,
        bodyB: PhysicsCollisionObject,
        point: ContactManifoldPoint
    ) {
        fun callEvent(thisBody: PhysicsCollisionObject, otherBody: PhysicsCollisionObject) {
            if (thisBody !is SokolPhysicsObject) return
            useSpaceOf(thisBody.entity) { space ->
                space.call(ColliderSystem.Contact(thisBody, otherBody, point))
            }
        }

        callEvent(bodyA, bodyB)
        callEvent(bodyB, bodyA)
    }

    private fun onPhysicsPostStep() {
        players.postPhysicsStep()
    }
}

inline fun <reified C : SokolComponent> Sokol.InitContext.persistentComponent(type: ComponentType<C>) {
    componentType(type)
    transientComponent<C>()
}

inline fun <reified C : SokolComponent> Sokol.InitContext.transientComponent() = componentClass(C::class)
