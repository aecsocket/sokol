package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.core.LogLevel
import com.gitlab.aecsocket.alexandria.core.LogList
import com.gitlab.aecsocket.alexandria.core.extension.force
import com.gitlab.aecsocket.alexandria.core.extension.register
import com.gitlab.aecsocket.alexandria.core.extension.registerExact
import com.gitlab.aecsocket.alexandria.core.extension.walkPathed
import com.gitlab.aecsocket.alexandria.core.keyed.Registry
import com.gitlab.aecsocket.alexandria.paper.AlexandriaAPI
import com.gitlab.aecsocket.alexandria.paper.BasePlugin
import com.gitlab.aecsocket.alexandria.paper.extension.bukkitPlayers
import com.gitlab.aecsocket.alexandria.paper.extension.scheduleRepeating
import com.gitlab.aecsocket.sokol.core.*
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.serialize.SerializationException

private const val BLUEPRINT = "blueprint"
private const val ITEMS = "items"

private lateinit var instance: Sokol
val SokolAPI get() = instance

class Sokol : BasePlugin() {
    @ConfigSerializable
    data class Settings(
        val enabled: Boolean = false
    )

    lateinit var settings: Settings private set

    private val _componentTypes = HashMap<Key, SokolComponentType>()
    val componentTypes: Map<Key, SokolComponentType> get() = _componentTypes

    private val _itemBlueprints = Registry.create<ItemBlueprint>()
    val itemBlueprints: Registry<ItemBlueprint> get() = _itemBlueprints

    val persistence = SokolPersistence(this)
    val engine = SokolEngine()
    val entityResolver = EntityResolver(this)

    init {
        instance = this
    }

    override fun onEnable() {
        super.onEnable()
        SokolCommand(this)
        AlexandriaAPI.registerConsumer(this,
            onInit = {
                serializers
                    .register(SokolComponent::class, ComponentSerializer(this@Sokol))
                    .registerExact(ItemBlueprint::class, ItemBlueprintSerializer(this@Sokol))
            },
            onLoad = {
                addDefaultI18N()
            }
        )

        registerComponentType(TestComponentType())

        engine.addSystem {
            bukkitPlayers.forEach {
                it.sendActionBar(Component.text("entities = ${engine.entities.size}"))
            }
        }

        scheduleRepeating {
            engine.clearEntities()
            entityResolver.resolve { engine.addEntity(it) }
            engine.update()
        }
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

            val blueprintDir = dataFolder.resolve(BLUEPRINT)
            blueprintDir.walkPathed { file, _, _ ->
                if (file.isFile) {
                    val node = AlexandriaAPI.configLoader().file(file).build().load()
                    node.node(ITEMS).childrenMap().forEach { (_, child) ->
                        _itemBlueprints.register(child.force<ItemBlueprint>())
                    }
                }
                true
            }

            return true
        }
        return false
    }

    fun registerComponentType(type: SokolComponentType) {
        _componentTypes[type.key] = type
    }
}
