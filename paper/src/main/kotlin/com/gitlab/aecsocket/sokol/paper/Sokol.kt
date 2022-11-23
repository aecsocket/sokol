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

typealias InputListener = (event: PlayerInputEvent) -> Unit

class Sokol : BasePlugin(), SokolAPI {
    @ConfigSerializable
    data class Settings(
        val enableBstats: Boolean = true,
        val resolveContainerBlocks: Boolean = true,
        val resolveContainerItems: Boolean = true,
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

    internal val mobsAdded = HashSet<Int>()
    private val registrations = ArrayList<Registration>()
    private val onInput = ArrayList<InputListener>()
    internal var hasReloaded = false

    override fun onEnable() {
        super.onEnable()
        command = SokolCommand(this)
        CraftBulletAPI.onContact(::onContact)
        AlexandriaAPI.registerConsumer(this,
            onInit = {
                serializers
                    .registerExact(ComponentProfileSerializer(this@Sokol))
                    .registerExact(EntityProfileSerializer)
                    .registerExact(KeyedEntityProfileSerializer)
                    .registerExact(MeshesStatic.MeshDefinitionSerializer)
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

            log.line(LogLevel.Info) { "Set up ${engineBuilder.countComponentTypes()} component types, ${engineBuilder.countSystemFactories()} systems" }

            val postInitCtx = object : PostInitContext {}
            registrations.forEach { it.onPostInit(postInitCtx) }

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
                    .systemFactory { DisplayNameProfileSystem(it) }
                    .systemFactory { MobPositionSystem(it) }
                    .systemFactory { CompositeSystem(it) }
                    .systemFactory { LocalTransformTarget }
                    .systemFactory { LocalTransformStaticSystem(it) }
                    .systemFactory { RootLocalTransformTarget }
                    .systemFactory { PositionPreTarget }
                    .systemFactory { PositionTarget }
                    .systemFactory { PositionSystem(it) }
                    .systemFactory { VelocityTarget }
                    .systemFactory { PlayerTrackedTarget }
                    .systemFactory { PlayerTrackedSystem(it) }
                    .systemFactory { RemovablePreTarget }
                    .systemFactory { RemovableTarget }
                    .systemFactory { RemovableSystem(it) }
                    .systemFactory { MobConstructorSystem(this@Sokol, it) }
                    .systemFactory { PositionEffectsSystem(it) }
                    .systemFactory { InputCallbacksInstanceTarget }
                    .systemFactory { InputCallbacksSystem(it) }
                    .systemFactory { InputCallbacksInstanceSystem(it) }
                    .systemFactory { TakeableSystem(this@Sokol, it) }
                    .systemFactory { MeshesTarget }
                    .systemFactory { MeshesStaticSystem(it) }
                    .systemFactory { MeshesItemSystem(this@Sokol, it) }
                    .systemFactory { MeshesInWorldSystem(it) }
                    .systemFactory { MeshesInWorldMobSystem(it) }
                    .systemFactory { HoverMeshGlowSystem(it) }
                    .systemFactory { ColliderInstanceTarget }
                    .systemFactory { ColliderConstructSystem(it) }
                    .systemFactory { ColliderSystem(it) }
                    .systemFactory { ColliderInstanceSystem(it) }
                    .systemFactory { ColliderInstanceParentSystem(it) }
                    .systemFactory { ColliderInstancePositionSystem(it) }
                    .systemFactory { ColliderMobSystem(it) }
                    .systemFactory { ColliderMobPositionSystem(it) }
                    .systemFactory { ColliderEffectsSystem(it) }
                    .systemFactory { HoldableSystem(this@Sokol, it) }
                    .systemFactory { HoldableItemSystem(this@Sokol, it) }
                    .systemFactory { HeldPositionSystem(it) }
                    .systemFactory { HeldMobSystem(this@Sokol, it) }

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
                    .componentType<DisplayNameProfile>()
                    .componentType<MobPosition>()
                    .componentType<AsMob>()
                    .componentType<AsItem>()
                    .componentType<IsRoot>()
                    .componentType<IsChild>()
                    .componentType<ContainerMap>()
                    .componentType<LocalTransform>()
                    .componentType<LocalTransformStatic>()
                    .componentType<Rotation>()
                    .componentType<RootLocalTransform>()
                    .componentType<PositionRead>()
                    .componentType<PositionWrite>()
                    .componentType<VelocityRead>()
                    .componentType<PlayerTracked>()
                    .componentType<Removable>()
                    .componentType<InputCallbacks>()
                    .componentType<InputCallbacksInstance>()
                    .componentType<Takeable>()
                    .componentType<PositionEffects>()
                    .componentType<Meshes>()
                    .componentType<MeshesStatic>()
                    .componentType<MeshesItem>()
                    .componentType<MeshesInWorld>()
                    .componentType<HoverMeshGlow>()
                    .componentType<Collider>()
                    .componentType<ColliderInstance>()
                    .componentType<RigidBodyCollider>()
                    .componentType<VehicleBodyCollider>()
                    .componentType<GhostBodyCollider>()
                    .componentType<ColliderEffects>()
                    .componentType<Holdable>()
                    .componentType<Held>()
                registerComponentType(DisplayNameProfile.Type)
                registerComponentType(AsMob.Type)
                registerComponentType(AsItem.Type)
                registerComponentType(ContainerMap.Type(this@Sokol))
                registerComponentType(LocalTransformStatic.Type)
                registerComponentType(Rotation.Type)
                registerComponentType(InputCallbacks.Type)
                registerComponentType(Takeable.Type)
                registerComponentType(PositionEffects.Type)
                registerComponentType(MeshesStatic.Type)
                registerComponentType(MeshesItem.Type)
                registerComponentType(MeshesInWorld.Type)
                registerComponentType(HoverMeshGlow.Type)
                registerComponentType(Collider.Type)
                registerComponentType(RigidBodyCollider.Type)
                registerComponentType(VehicleBodyCollider.Type)
                registerComponentType(GhostBodyCollider.Type)
                registerComponentType(ColliderEffects.Type)
                registerComponentType(Holdable.Type)
            }
        )
    }

    private fun onContact(
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
}
