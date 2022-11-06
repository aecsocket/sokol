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
import com.gitlab.aecsocket.sokol.core.EntityBlueprintSerializer
import com.gitlab.aecsocket.sokol.paper.component.*
import com.gitlab.aecsocket.sokol.paper.component.Meshes
import net.kyori.adventure.key.Key
import org.bstats.bukkit.Metrics
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataContainer
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
    fun handle(event: PlayerInput)
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

    lateinit var settings: Settings private set
    override lateinit var engine: SokolEngine private set
    lateinit var space: SokolSpace private set

    private val _componentTypes = HashMap<String, ComponentType>()
    val componentTypes: Map<String, ComponentType> get() = _componentTypes

    private val _entityProfiles = Registry.create<KeyedEntityProfile>()
    val entityProfiles: Registry<KeyedEntityProfile> get() = _entityProfiles

    override val persistence = PaperSokolPersistence(this)
    val entityResolver = EntityResolver(this)
    val entityHoster = EntityHoster(this)
    val entityHover = EntityHover(this)
    val engineTimings = Timings(60 * 1000)
    val entityHolding = EntityHolding()

    internal val mobsAdded = HashSet<Int>()
    private val registrations = ArrayList<Registration>()
    private val inputHandlers = ArrayList<SokolInputHandler>()
    private var hasReloaded = false

    override fun onEnable() {
        super.onEnable()
        SokolCommand(this)
        AlexandriaAPI.registerConsumer(this,
            onInit = {
                serializers
                    .registerExact(ComponentProfileSerializer(this@Sokol))
                    .registerExact(EntityProfileSerializer)
                    .registerExact(KeyedEntityProfileSerializer)
                    .register(EntityBlueprintSerializer(this@Sokol))
                    .register(SokolEntitySerializer)
                    .registerExact(Meshes.PartDefinitionSerializer)
                    .register(CompositePathSerializer)
                    .register(DeltaSerializer)
            },
            onLoad = {
                addDefaultI18N()
            }
        )
        AlexandriaAPI.inputHandler { event ->
            val sokolEvent = PlayerInput(event)
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

            entityResolver.enable()
            entityHoster.enable()
            entityHover.enable()
            space = SokolSpace(engine)

            log.line(LogLevel.Info) { "Set up ${engineBuilder.countComponentTypes()} component types, ${engineBuilder.countSystemFactories()} systems" }

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

    fun usePDC(
        pdc: PersistentDataContainer,
        write: Boolean = true,
        builder: (EntityBlueprint) -> Unit,
        consumer: (SokolEntity) -> Unit,
    ): Boolean {
        persistence.getTag(pdc, persistence.entityKey)?.let { tag ->
            val blueprint = persistence.readBlueprint(tag)
            builder(blueprint)

            val entity = engine.buildEntity(blueprint)
            consumer(entity)

            if (write) {
                persistence.writeEntityDeltas(entity, tag)
            }
            return true
        }
        return false
    }

    fun useMob(
        mob: Entity,
        write: Boolean = true,
        builder: (EntityBlueprint) -> Unit = {},
        consumer: (SokolEntity) -> Unit,
    ): Boolean {
        return usePDC(mob.persistentDataContainer, write,
            { blueprint ->
                blueprint.components.set(hostedByMob(mob))
                builder(blueprint)
            },
            consumer
        )
    }

    fun useItem(
        item: ItemStack,
        write: Boolean = true,
        builder: (EntityBlueprint) -> Unit = {},
        consumer: (SokolEntity) -> Unit,
    ): Boolean {
        if (!item.hasItemMeta()) return false
        val meta = item.itemMeta
        var dirty = false
        return usePDC(meta.persistentDataContainer, write,
            { blueprint ->
                blueprint.components.set(object : HostedByItem {
                    override val item get() = item

                    override fun <R> readMeta(action: (ItemMeta) -> R): R {
                        return action(meta)
                    }

                    override fun writeMeta(action: (ItemMeta) -> Unit) {
                        action(meta)
                        dirty = true
                    }

                    override fun toString() = "HostedByItem($item)"
                })
                builder(blueprint)
            }
        ) { entity ->
            consumer(entity)
            if (write && dirty) {
                item.itemMeta = meta
            }
        }
    }

    fun usePlayerItems(
        player: Player,
        write: Boolean = true,
        consumer: (SokolEntity) -> Unit
    ) {
        player.inventory.forEachIndexed { idx, stack ->
            stack?.let {
                useItem(stack, write,
                    { blueprint ->
                        blueprint.components.set(ItemHolder.byPlayer(player, idx))
                    }
                ) { entity ->
                    consumer(entity)
                }
            }
        }
    }

    private fun registerDefaultConsumer() {
        registerConsumer(
            onInit = {
                engine
                    .systemFactory { HostedByWorldTarget }
                    .systemFactory { HostedByChunkTarget }
                    .systemFactory { HostedByMobTarget }
                    .systemFactory { HostedByBlockTarget }
                    .systemFactory { HostedByItemFormTarget }
                    .systemFactory { HostedByItemTarget }
                    .systemFactory { MobInjectorSystem(this@Sokol, it) }
                    .systemFactory { CompositeSystem(it) }
                    .systemFactory { RotationSystem(it) }
                    .systemFactory { PositionTarget }
                    .systemFactory { PositionSystem(it) }
                    .systemFactory { SupplierTrackedPlayersTarget }
                    .systemFactory { SupplierTrackedPlayersBuildSystem(it) }
                    .systemFactory { CompositeTransformSystem(it) }
                    .systemFactory { PositionSystem(it) }
                    .systemFactory { ColliderBuildSystem(it) }
                    .systemFactory { ColliderSystem(it) }
                    .systemFactory { MeshesSystem(it) }
                    .systemFactory { MeshesInWorldForwardSystem(it) }
                    .systemFactory { MeshesInWorldSystem(it) }
                    .systemFactory { MeshesStaticSystem(it) }
                    .systemFactory { MeshesItemSystem(this@Sokol, it) }
                    .systemFactory { ItemNameSystem(it) }
                    .systemFactory { ItemNameStaticSystem(it) }
                    .systemFactory { ItemNameProfileSystem(it) }
                    .systemFactory { OnInputSystem(this@Sokol, it) }
                    .systemFactory { OnInputInstanceSystem(it) }
                    .systemFactory { HoverGlowSystem(it) }
                    .systemFactory { HoverGlowCallerSystem(it) }
                    .systemFactory { TakeableSystem(this@Sokol, it) }
                    .systemFactory { HoldableTarget }
                    .systemFactory { HoldableSystem(it) }
                    .systemFactory { HoldableItemSystem(this@Sokol, it) }
                    .systemFactory { HoldableMobSystem(this@Sokol, it) }
                    .systemFactory { HoldableGlowSystem(it) }
                    .systemFactory { AttachableSystem(it) }

                    .componentType<HostedByWorld>()
                    .componentType<HostedByChunk>()
                    .componentType<HostedByMob>()
                    .componentType<HostedByBlock>()
                    .componentType<HostedByItem>()
                    .componentType<ItemHolder>()
                    .componentType<Hovered>()
                    .componentType<PositionRead>()
                    .componentType<PositionWrite>()
                    .componentType<Removable>()
                    .componentType<SupplierTrackedPlayers>()
                    .componentType<SupplierEntityAccess>()
                    .componentType<HostableByMob>()
                    .componentType<HostableByItem>()
                    .componentType<Composite>()
                    .componentType<CompositeChild>()
                    .componentType<AsChildTransform>()
                    .componentType<CompositeTransform>()
                    .componentType<EntitySlot>()
                    .componentType<Rotation>()
                    .componentType<Collider>()
                    .componentType<RigidBody>()
                    .componentType<VehicleBody>()
                    .componentType<GhostBody>()
                    .componentType<Meshes>()
                    .componentType<MeshesStatic>()
                    .componentType<MeshesItem>()
                    .componentType<MeshesInWorld>()
                    .componentType<ItemName>()
                    .componentType<ItemNameStatic>()
                    .componentType<ItemNameProfile>()
                    .componentType<OnInput>()
                    .componentType<OnInputInstance>()
                    .componentType<HoverGlow>()
                    .componentType<Takeable>()
                    .componentType<Holdable>()
                    .componentType<HoldableGlow>()
                    .componentType<Attachable>()
                registerComponentType(HostableByMob.Type)
                registerComponentType(HostableByItem.Type)
                registerComponentType(Composite.Type(this@Sokol))
                registerComponentType(AsChildTransform.Type)
                registerComponentType(EntitySlot.Type)
                registerComponentType(Rotation.Type)
                registerComponentType(Collider.Type)
                registerComponentType(RigidBody.Type)
                registerComponentType(VehicleBody.Type)
                registerComponentType(GhostBody.Type)
                registerComponentType(MeshesStatic.Type)
                registerComponentType(MeshesItem.Type)
                registerComponentType(MeshesInWorld.Type)
                registerComponentType(ItemNameStatic.Type)
                registerComponentType(ItemNameProfile.Type)
                registerComponentType(OnInput.Type(this@Sokol))
                registerComponentType(HoverGlow.Type)
                registerComponentType(Takeable.Type)
                registerComponentType(Holdable.Type)
                registerComponentType(HoldableGlow.Type)
                registerComponentType(Attachable.Type)
            }
        )
    }
}
