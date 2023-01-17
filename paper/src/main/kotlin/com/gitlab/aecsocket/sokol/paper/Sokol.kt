package com.gitlab.aecsocket.sokol.paper

import com.github.retrooper.packetevents.PacketEvents
import com.gitlab.aecsocket.alexandria.core.LogLevel
import com.gitlab.aecsocket.alexandria.core.LogList
import com.gitlab.aecsocket.alexandria.core.extension.*
import com.gitlab.aecsocket.alexandria.core.keyed.*
import com.gitlab.aecsocket.alexandria.paper.*
import com.gitlab.aecsocket.alexandria.paper.effect.ParticleEffect
import com.gitlab.aecsocket.alexandria.paper.extension.*
import com.gitlab.aecsocket.craftbullet.core.ContactManifoldPoint
import com.gitlab.aecsocket.craftbullet.core.ServerPhysicsSpace
import com.gitlab.aecsocket.craftbullet.core.Timings
import com.gitlab.aecsocket.craftbullet.paper.CraftBulletAPI
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.component.*
import com.gitlab.aecsocket.sokol.paper.stat.NumberStatBarFormatter
import com.gitlab.aecsocket.sokol.paper.stat.NumberStatFormatter
import com.gitlab.aecsocket.sokol.paper.stat.NameStatFormatter
import com.jme3.bullet.collision.PhysicsCollisionObject
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
            onInit = {
                serializers
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
            onLoad = {
                addDefaultI18N()
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
        onInit: InitContext.() -> Unit = {},
        onPostInit: PostInitContext.() -> Unit = {},
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
            onInit = {
                system { IsWorldTarget }
                system { IsChunkTarget }
                system { IsMobTarget }
                system { IsBlockTarget }
                system { IsItemFormTarget }
                system { IsItemTarget }
                system { PersistenceSystem(sokol, it) }
                system { BlockPersistSystem(it) }
                system { ItemPersistSystem(sokol, it) }
                system { ItemTagPersistSystem(it) }
                system { DisplayNameTarget }
                system { DisplayNameFromProfileSystem(it) }
                system { MobConstructorSystem(sokol, it) }
                system { MobSystem(it) }
                system { DeltaTransformTarget }
                system { DeltaTransformStaticSystem(it) }
                system { PositionAccessTarget }
                system { VelocityAccessTarget }
                system { PositionFromDeltaSystem(it) }
                system { PlayerTrackedTarget }
                system { PlayerTrackedSystem(it) }
                system { RemovablePreTarget }
                system { RemovableTarget }
                system { RemovableSystem(it) }
                system { PositionEffectsSystem(it) }
                system { InputCallbacksInstanceTarget }
                system { InputCallbacksSystem(it) }
                system { InputCallbacksInstanceSystem(it) }
                system { InputRemovableSystem(it) }
                system { TakeableAsItemSystem(sokol, it) }
                system { MeshProviderTarget }
                system { MeshProviderStaticSystem(it) }
                system { MeshProviderFromItemSystem(sokol, it) }
                system { MeshesInWorldInstanceTarget }
                system { MeshesInWorldSystem(it) }
                system { MeshesInWorldInstanceSystem(it) }
                system { MeshesInWorldMobSystem(sokol, it) }
                system { HoverMeshGlowSystem(it) }
                system { ColliderInstanceTarget }
                system { ColliderSystem(it) }
                system { ColliderInstanceSystem(it) }
                system { ColliderInstanceParentSystem(it) }
                system { ColliderMobSystem(sokol, it) }
                system { ColliderEffectsSystem(it) }
                system { HoldableInputsSystem(sokol, it) }
                system { PlaceableAsMobSystem(sokol, it) }
                system { HeldColliderSystem(it) }
                system { HeldMobSystem(sokol, it) }
                system { HoldMovableCallbackSystem(sokol, it) }
                system { HoldMovableColliderSystem(it) }
                system { HoldDetachableCallbackSystem(sokol, it) }
                system { HoldDetachableColliderSystem(it) }
                system { HeldSnapSystem(it) }
                system { HeldAttachableSystem(it) }
                system { HeldAttachableInputsSystem(sokol, it) }
                system { HeldAttachableEffectsSystem(it) }
                system { HeldMeshGlowSystem(it) }
                system { EntitySlotTarget }
                system { EntitySlotInMapSystem(it) }
                system { StatsInstanceTarget }
                system { StatsSystem(it) }
                system { ItemDisplayNameSystem(it) }
                system { ItemLoreManagerSystem(it) }
                system { ItemLoreStaticSystem(it) }
                system { ItemLoreFromProfileSystem(it) }
                system { ItemLoreStatsSystem(it) }

                componentClass<Profiled>()
                componentClass<Composite>()
                componentClass<InTag>()
                componentClass<IsWorld>()
                componentClass<IsChunk>()
                componentClass<IsMob>()
                componentClass<IsBlock>()
                componentClass<IsItem>()
                componentClass<ItemHolder>()
                componentClass<InItemTag>()
                componentClass<DisplayName>()
                persistentComponent(DisplayNameFromProfile.Type)
                persistentComponent(AsMob.Type)
                persistentComponent(AsItem.Type)
                componentClass<IsChild>()
                persistentComponent(components.containerMap)
                componentClass<DeltaTransform>()
                persistentComponent(DeltaTransformStatic.Type)
                persistentComponent(Rotation.Type)
                componentClass<PositionAccess>()
                componentClass<VelocityRead>()
                persistentComponent(PositionFromDelta.Type)
                componentClass<PlayerTracked>()
                componentClass<Removable>()
                persistentComponent(InputCallbacks.Type)
                componentClass<InputCallbacksInstance>()
                persistentComponent(InputRemovable.Type)
                persistentComponent(TakeableAsItem.Type)
                persistentComponent(PositionEffects.Type)
                componentClass<MeshProvider>()
                persistentComponent(MeshProviderStatic.Type)
                persistentComponent(MeshProviderFromItem.Type)
                persistentComponent(MeshesInWorld.Type)
                componentClass<MeshesInWorldInstance>()
                persistentComponent(HoverShape.Type)
                persistentComponent(HoverMeshGlow.Type)
                persistentComponent(Collider.Type)
                componentClass<ColliderInstance>()
                persistentComponent(ColliderRigidBody.Type)
                persistentComponent(ColliderVehicleBody.Type)
                persistentComponent(ColliderEffects.Type)
                persistentComponent(Holdable.Type)
                componentClass<Held>()
                persistentComponent(PlaceableAsMob.Type)
                persistentComponent(HoldMovable.Type)
                persistentComponent(HoldDetachable.Type)
                persistentComponent(HeldSnap.Type)
                persistentComponent(HeldAttachable.Type)
                persistentComponent(HeldAttachableEffects.Type)
                persistentComponent(HeldMeshGlow.Type)
                componentClass<EntitySlot>()
                persistentComponent(EntitySlotInMap.Type)
                persistentComponent(components.stats)
                componentClass<StatsInstance>()
                persistentComponent(ItemDisplayName.Type)
                persistentComponent(ItemLoreManager.Type)
                persistentComponent(ItemLoreStatic.Type)
                persistentComponent(ItemLoreFromProfile.Type)
                persistentComponent(components.itemLoreStats)

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
    componentClass<C>()
}

inline fun <reified C : SokolComponent> Sokol.InitContext.componentClass() = componentClass(C::class)
