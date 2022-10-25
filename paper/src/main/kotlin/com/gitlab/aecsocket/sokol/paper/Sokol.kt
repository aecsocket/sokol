package com.gitlab.aecsocket.sokol.paper

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.gitlab.aecsocket.alexandria.core.LogLevel
import com.gitlab.aecsocket.alexandria.core.LogList
import com.gitlab.aecsocket.alexandria.core.extension.*
import com.gitlab.aecsocket.alexandria.core.input.Input
import com.gitlab.aecsocket.alexandria.core.keyed.*
import com.gitlab.aecsocket.alexandria.core.physics.Quaternion
import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.*
import com.gitlab.aecsocket.alexandria.paper.extension.location
import com.gitlab.aecsocket.alexandria.paper.extension.position
import com.gitlab.aecsocket.alexandria.paper.extension.registerEvents
import com.gitlab.aecsocket.alexandria.paper.extension.scheduleRepeating
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.util.Timings
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
import java.nio.file.Path

private const val CONFIG = "config"
private const val ITEMS = "items"
private const val MOBS = "mobs"

private const val BSTATS_ID = 11870
internal const val TIMING_MAX_MEASUREMENTS = 60 * TPS

private lateinit var instance: Sokol
val SokolAPI get() = instance

class Sokol : BasePlugin() {
    @ConfigSerializable
    data class Settings(
        val enableBstats: Boolean = true,
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
    lateinit var space: SokolSpace private set

    private val _componentTypes = HashMap<String, PersistentComponentType>()
    val componentTypes: Map<String, PersistentComponentType> get() = _componentTypes

    private val _itemBlueprints = Registry.create<KeyedItemBlueprint>()
    val itemBlueprints: Registry<KeyedItemBlueprint> get() = _itemBlueprints

    private val _mobBlueprints = Registry.create<KeyedMobBlueprint>()
    val mobBlueprints: Registry<KeyedMobBlueprint> get() = _mobBlueprints

    val persistence = SokolPersistence(this)
    val entityResolver = EntityResolver(this)
    val engineTimings = Timings(TIMING_MAX_MEASUREMENTS)
    val itemPlacing = ItemPlacing(this)

    val hostableByItem = HostableByItem.Type()
    val colliders = Collider.Type()
    val staticMeshes = StaticMeshes.Type()
    val simplePlaceables = SimplePlaceable.Type()
    val registryComponentTypes = listOf(hostableByItem, colliders, staticMeshes, simplePlaceables)

    internal val mobsAdded = HashSet<Int>()
    private val registrations = ArrayList<Registration>()
    private var hasReloaded = false

    override fun onEnable() {
        super.onEnable()
        SokolCommand(this)
        AlexandriaAPI.registerConsumer(this,
            onInit = {
                serializers
                    .register(ComponentSerializer(this@Sokol))
                    .register(ComponentFactorySerializer(this@Sokol))
                    .registerExact(BlueprintSerializer(this@Sokol))
                    .registerExact(ItemBlueprintSerializer(this@Sokol))
                    .registerExact(EntityBlueprintSerializer(this@Sokol))
                    .registerExact(Meshes.PartDefinitionSerializer)
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

        itemPlacing.enable()
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

            space = SokolSpace(engine)

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

            if (this.settings.enableBstats) {
                Metrics(this, BSTATS_ID)
            }

            try {
                entityResolver.load(settings)
            } catch (ex: Exception) {
                log.line(LogLevel.Error, ex) { "Could not load entity resolver settings" }
                return false
            }

            _itemBlueprints.clear()
            _mobBlueprints.clear()
            registryComponentTypes.forEach { it.registry.clear() }

            val configs = walkConfigs(dataFolder.resolve(CONFIG),
                onError = { ex, path ->
                    log.line(LogLevel.Warning, ex) { "Could not load config from $path" }
                }
            )

            fun loadConfigs(
                makeMessage: (Path) -> String,
                action: (ConfigurationNode) -> Unit,
            ) {
                configs.forEach { (node, path) ->
                    try {
                        action(node)
                    } catch (ex: Exception) {
                        log.line(LogLevel.Warning, ex) { makeMessage(path) }
                    }
                }
            }

            registryComponentTypes.forEach { type ->
                loadConfigs({ "Could not read config for ${type::class.simpleName} from $it" }) {
                    type.load(it)
                }
            }

            loadConfigs({ "Could not read item blueprints from $it" }) {
                it.node(ITEMS).childrenMap().forEach { (_, child) ->
                    _itemBlueprints.register(child.force())
                }
            }
            loadConfigs({ "Could not read mob blueprints from $it" }) {
                it.node(MOBS).childrenMap().forEach { (_, child) ->
                    _mobBlueprints.register(child.force())
                }
            }

            log.line(LogLevel.Info) { "Loaded ${_mobBlueprints.size} mob blueprints, ${_itemBlueprints.size} item blueprints" }

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

    fun usePDC(
        pdc: PersistentDataContainer,
        write: Boolean = true,
        builder: (SokolEntityBuilder) -> Unit,
        consumer: (SokolEntityAccess) -> Unit,
    ): Boolean {
        persistence.getTag(pdc, persistence.entityKey)?.let { tag ->
            val blueprint = persistence.readBlueprint(tag)
            if (blueprint.isNotEmpty()) {
                val entity = blueprint
                    .build(engine).also(builder)
                    .build().also(consumer)
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

    fun useMob(
        mob: Entity,
        write: Boolean = true,
        builder: (SokolEntityBuilder) -> Unit = {},
        consumer: (SokolEntityAccess) -> Unit,
    ): Boolean {
        return usePDC(mob.persistentDataContainer, write,
            {
                entityResolver.populate(SokolObjectType.Mob, it, mob)
                builder(it)
            },
            consumer
        )
    }

    fun useItem(
        item: ItemStack,
        write: Boolean = true,
        builder: (SokolEntityBuilder) -> Unit = {},
        consumer: (SokolEntityAccess) -> Unit,
    ): Boolean {
        if (!item.hasItemMeta()) return false
        val meta = item.itemMeta
        var dirty = false
        return usePDC(meta.persistentDataContainer, write, {
            entityResolver.populate(SokolObjectType.Item, it, ItemData({ item }, { meta }))
            it.setComponent(object : HostedByItem {
                override val item get() = item

                override fun <R> readMeta(action: (ItemMeta) -> R): R {
                    return action(meta)
                }

                override fun writeMeta(action: (ItemMeta) -> Unit) {
                    action(meta)
                    dirty = true
                }
            })
            builder(it)
        }) { entity ->
            consumer(entity)
            if (write && dirty) {
                item.itemMeta = meta
            }
        }
    }

    fun usePlayerItems(
        player: Player,
        write: Boolean = true,
        consumer: (SokolEntityAccess) -> Unit
    ) {
        player.inventory.forEachIndexed { idx, stack ->
            stack?.let {
                useItem(stack, write,
                    {
                        it.setComponent(ItemHolder.byPlayer(player, idx))
                    }
                ) { entity ->
                    consumer(entity)
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
                    entity.call(ItemEvent.PlayerInput(input, player) { event.cancel() })
                }
            }
        }
    }

    private fun registerDefaultConsumer() {
        registerConsumer(
            onInit = {
                engine
                    .systemFactory { it.define(ColliderSystem(it)) }
                    .systemFactory { it.define(MeshesSystem(it)) }
                    .systemFactory { it.define(StaticMeshesInjectorSystem(it)) }
                    .systemFactory { it.define(ItemNameSystem(it)) }
                    .systemFactory { it.define(PlaceableSystem(this@Sokol, it)) }
                    .systemFactory { it.define(SimplePlaceableInjectorSystem(it)) }

                    .componentType<HostedByWorld>()
                    .componentType<HostedByChunk>()
                    .componentType<HostedByMob>()
                    .componentType<HostedByBlock>()
                    .componentType<HostedByItem>()
                    .componentType<Position>()
                    .componentType<IsValidSupplier>()
                    .componentType<ItemHolder>()

                    .componentType<HostableByItem>()
                    .componentType<HostableByMob>()
                    .componentType<Rotation>()
                    .componentType<Collider>()
                    .componentType<RigidBody>()
                    .componentType<VehicleBody>()
                    .componentType<Meshes>()
                    .componentType<StaticMeshes>()
                    .componentType<ItemName>()
                    .componentType<Placeable>()
                    .componentType<SimplePlaceable>()
                registerComponentType(hostableByItem)
                registerComponentType(HostableByMob.Type)
                registerComponentType(Rotation.Type)
                registerComponentType(colliders)
                registerComponentType(RigidBody.Type)
                registerComponentType(VehicleBody.Type)
                registerComponentType(Meshes.Type)
                registerComponentType(staticMeshes)
                registerComponentType(ItemName.Type)
                registerComponentType(Placeable.Type)
                registerComponentType(simplePlaceables)
            },
            onPostInit = {
                val mRotation = engine.componentMapper<Rotation>()

                entityResolver.populator(SokolObjectType.World) { builder, world ->
                    builder.setComponent(hostedByWorld(world))
                }

                entityResolver.populator(SokolObjectType.Chunk) { builder, chunk ->
                    builder.setComponent(hostedByChunk(chunk))
                }

                entityResolver.populator(SokolObjectType.Mob) { builder, mob ->
                    builder.setComponent(hostedByMob(mob))

                    builder.setComponent(object : IsValidSupplier {
                        override val valid: () -> Boolean get() = { mob.isValid }
                    })

                    val rotation = mRotation.mapOr(builder)
                    var transform = Transform(mob.location.position(), rotation?.rotation ?: Quaternion.Identity)
                    builder.setComponent(object : Position {
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

                // populators for blocks and items are a bit more complicated
            }
        )
    }
}
