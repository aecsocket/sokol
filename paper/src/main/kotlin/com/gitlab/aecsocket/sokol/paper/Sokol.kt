package com.gitlab.aecsocket.sokol.paper

import com.github.retrooper.packetevents.PacketEvents
import com.gitlab.aecsocket.alexandria.core.LogLevel
import com.gitlab.aecsocket.alexandria.core.LogList
import com.gitlab.aecsocket.alexandria.core.extension.*
import com.gitlab.aecsocket.alexandria.core.keyed.*
import com.gitlab.aecsocket.alexandria.paper.*
import com.gitlab.aecsocket.alexandria.paper.extension.*
import com.gitlab.aecsocket.craftbullet.core.ContactManifoldPoint
import com.gitlab.aecsocket.craftbullet.core.ServerPhysicsSpace
import com.gitlab.aecsocket.craftbullet.core.Timings
import com.gitlab.aecsocket.craftbullet.paper.CraftBulletAPI
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.component.*
import com.jme3.bullet.collision.PhysicsCollisionObject
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.format.TextColor
import org.bstats.bukkit.Metrics
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.serialize.SerializationException

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
        val entityHoverDistance: Double = 0.0,
        val drawRadius: Double = 16.0,
        val drawHoverShape: ParticleEngineEffect = ParticleEngineEffect.Empty,
        val drawSlots: ParticleEngineEffect = ParticleEngineEffect.Empty,
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
    lateinit var space: SokolEntityContainer private set

    private val _componentTypes = HashMap<String, ComponentType>()
    val componentTypes: Map<String, ComponentType> get() = _componentTypes

    private val _entityProfiles = Registry.create<KeyedEntityProfile>()
    val entityProfiles: Registry<KeyedEntityProfile> get() = _entityProfiles

    override val persistence = PaperSokolPersistence(this)
    val resolver = EntityResolver(this)
    val hoster = EntityHoster(this)
    val timings = Timings(60 * 1000)
    val holding = EntityHolding(this)
    val players = SokolPlayers(this)

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
                    .registerExact(ComponentProfileSerializer(this@Sokol))
                    .registerExact(EntityProfileSerializer)
                    .registerExact(KeyedEntityProfileSerializer)
                    .registerExact(MeshProviderStatic.MeshDefinitionSerializer)
                    .registerExact(EntitySerializer(this@Sokol))
                    .registerExact(BlueprintSerializer(this@Sokol))
                    .register(DeltaSerializer)
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
        PacketEvents.getAPI().eventManager.registerListener(SokolPacketListener(this@Sokol))

        CraftBulletAPI.onContact(::onPhysicsContact)
        CraftBulletAPI.onPostStep(::onPhysicsPostStep)
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
            space = engine.newEntityContainer()
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

    fun useSpace(capacity: Int = 64, write: Boolean = true, consumer: (SokolSpace) -> Unit) {
        val space = engine.newEntityContainer(capacity)
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
                    .systemFactory { DisplayNameTarget }
                    .systemFactory { DisplayNameFromProfileSystem(it) }
                    .systemFactory { MobConstructorSystem(this@Sokol, it) }
                    .systemFactory { MobSystem(it) }
                    .systemFactory { CompositeConstructSystem(it) }
                    .systemFactory { CompositeRootSystem(it) }
                    .systemFactory { DeltaTransformTarget }
                    .systemFactory { DeltaTransformStaticSystem(it) }
                    .systemFactory { PositionAccessTarget }
                    .systemFactory { VelocityAccessTarget }
                    .systemFactory { PositionFromDeltaSystem(it) }
                    .systemFactory { PlayerTrackedTarget }
                    .systemFactory { PlayerTrackedSystem(it) }
                    .systemFactory { RemovablePreTarget }
                    .systemFactory { RemovableTarget }
                    .systemFactory { RemovableSystem(it) }
                    .systemFactory { PositionEffectsSystem(it) }
                    .systemFactory { InputCallbacksInstanceTarget }
                    .systemFactory { InputCallbacksSystem(it) }
                    .systemFactory { InputCallbacksInstanceSystem(it) }
                    .systemFactory { InputRemovableSystem(it) }
                    .systemFactory { TakeableAsItemSystem(this@Sokol, it) }
                    .systemFactory { MeshProviderTarget }
                    .systemFactory { MeshProviderStaticSystem(it) }
                    .systemFactory { MeshProviderFromItemSystem(this@Sokol, it) }
                    .systemFactory { MeshesInWorldInstanceTarget }
                    .systemFactory { MeshesInWorldSystem(it) }
                    .systemFactory { MeshesInWorldInstanceSystem(it) }
                    .systemFactory { MeshesInWorldMobSystem(this@Sokol, it) }
                    .systemFactory { HoverMeshGlowSystem(it) }
                    .systemFactory { ColliderInstanceTarget }
                    .systemFactory { ColliderSystem(it) }
                    .systemFactory { ColliderInstanceSystem(it) }
                    .systemFactory { ColliderInstanceParentSystem(it) }
                    .systemFactory { ColliderInstancePositionSystem(it) }
                    .systemFactory { ColliderMobSystem(this@Sokol, it) }
                    .systemFactory { ColliderEffectsSystem(it) }
                    .systemFactory { HoldableInputsSystem(this@Sokol, it) }
                    .systemFactory { PlaceableAsMobSystem(this@Sokol, it) }
                    .systemFactory { HeldColliderSystem(it) }
                    .systemFactory { HeldMobSystem(this@Sokol, it) }
                    .systemFactory { HoldMovableCallbackSystem(this@Sokol, it) }
                    .systemFactory { HoldMovableColliderSystem(it) }
                    .systemFactory { HoldDetachableCallbackSystem(this@Sokol, it) }
                    .systemFactory { HoldDetachableColliderSystem(it) }
                    .systemFactory { HeldSnapSystem(it) }
                    .systemFactory { HeldAttachableSystem(it) }
                    .systemFactory { HeldAttachableInputsSystem(this@Sokol, it) }
                    .systemFactory { HeldAttachableEffectsSystem(it) }
                    .systemFactory { HeldMeshGlowSystem(it) }
                    .systemFactory { EntitySlotTarget }
                    .systemFactory { EntitySlotInMapSystem(it) }
                    .systemFactory { ItemDisplayNameSystem(it) }

                    .componentType<Profiled>()
                    .componentType<InTag>()
                    .componentType<IsWorld>()
                    .componentType<IsChunk>()
                    .componentType<IsMob>()
                    .componentType<IsBlock>()
                    .componentType<IsItem>()
                    .componentType<ItemHolder>()
                    .componentType<InItemTag>()
                    .componentType<DisplayName>()
                    .componentType<DisplayNameFromProfile>()
                    .componentType<AsMob>()
                    .componentType<AsItem>()
                    .componentType<IsRoot>()
                    .componentType<IsChild>()
                    .componentType<ContainerMap>()
                    .componentType<DeltaTransform>()
                    .componentType<DeltaTransformStatic>()
                    .componentType<Rotation>()
                    .componentType<PositionAccess>()
                    .componentType<VelocityRead>()
                    .componentType<PositionFromDelta>()
                    .componentType<PlayerTracked>()
                    .componentType<Removable>()
                    .componentType<InputCallbacks>()
                    .componentType<InputCallbacksInstance>()
                    .componentType<InputRemovable>()
                    .componentType<TakeableAsItem>()
                    .componentType<PositionEffects>()
                    .componentType<MeshProvider>()
                    .componentType<MeshProviderStatic>()
                    .componentType<MeshProviderFromItem>()
                    .componentType<MeshesInWorld>()
                    .componentType<MeshesInWorldInstance>()
                    .componentType<HoverShape>()
                    .componentType<HoverMeshGlow>()
                    .componentType<Collider>()
                    .componentType<ColliderInstance>()
                    .componentType<ColliderRigidBody>()
                    .componentType<ColliderVehicleBody>()
                    .componentType<ColliderEffects>()
                    .componentType<Holdable>()
                    .componentType<Held>()
                    .componentType<PlaceableAsMob>()
                    .componentType<HoldMovable>()
                    .componentType<HoldDetachable>()
                    .componentType<HeldSnap>()
                    .componentType<HeldAttachable>()
                    .componentType<HeldAttachableEffects>()
                    .componentType<HeldMeshGlow>()
                    .componentType<EntitySlot>()
                    .componentType<EntitySlotInMap>()
                    .componentType<ItemDisplayName>()
                    .componentType<Stats>()
                registerComponentType(DisplayNameFromProfile.Type)
                registerComponentType(AsMob.Type)
                registerComponentType(AsItem.Type)
                registerComponentType(ContainerMap.Type(this@Sokol))
                registerComponentType(DeltaTransformStatic.Type)
                registerComponentType(PositionFromDelta.Type)
                registerComponentType(Rotation.Type)
                registerComponentType(InputCallbacks.Type)
                registerComponentType(InputRemovable.Type)
                registerComponentType(TakeableAsItem.Type)
                registerComponentType(PositionEffects.Type)
                registerComponentType(MeshProviderStatic.Type)
                registerComponentType(MeshProviderFromItem.Type)
                registerComponentType(MeshesInWorld.Type)
                registerComponentType(HoverShape.Type)
                registerComponentType(HoverMeshGlow.Type)
                registerComponentType(Collider.Type)
                registerComponentType(ColliderRigidBody.Type)
                registerComponentType(ColliderVehicleBody.Type)
                registerComponentType(ColliderEffects.Type)
                registerComponentType(Holdable.Type)
                registerComponentType(PlaceableAsMob.Type)
                registerComponentType(HoldMovable.Type)
                registerComponentType(HoldDetachable.Type)
                registerComponentType(HeldSnap.Type)
                registerComponentType(HeldAttachable.Type)
                registerComponentType(HeldAttachableEffects.Type)
                registerComponentType(HeldMeshGlow.Type)
                registerComponentType(EntitySlotInMap.Type)
                registerComponentType(ItemDisplayName.Type)
                registerComponentType(Stats.Type)
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
            thisBody.entity.callSingle(ColliderSystem.Contact(thisBody, otherBody, point))
        }

        callEvent(bodyA, bodyB)
        callEvent(bodyB, bodyA)
    }

    private fun onPhysicsPostStep() {
        players.postPhysicsStep()
    }
}
