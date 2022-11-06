package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.force
import com.gitlab.aecsocket.alexandria.core.input.InputMapper
import com.gitlab.aecsocket.alexandria.paper.extension.getForPlayer
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.PlayerInput
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import net.kyori.adventure.key.Key
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required
import org.spongepowered.configurate.objectmapping.meta.Setting
import org.spongepowered.configurate.serialize.SerializationException
import kotlin.reflect.KClass

fun interface InputAction {
    fun run(event: PlayerInput): Boolean
}

data class OnInputInstance(
    var mapper: InputMapper<List<List<InputAction>>>
) : SokolComponent {
    override val componentType get() = OnInputInstance::class
}

data class OnInput(val profile: Profile) : MarkerPersistentComponent {
    companion object {
        val Key = SokolAPI.key("on_input")
    }

    override val componentType get() = OnInput::class
    override val key get() = Key

    @ConfigSerializable
    data class InputActionSettings(
        val hasAll: List<String> = emptyList(),
        val hasOne: List<String> = emptyList(),
        val hasNone: List<String> = emptyList(),
        @Required @Setting(value = "do") val doKeys: List<Key>
    )

    data class InputActionSettingsInstance(
        val filter: EntityFilter,
        val doKeys: List<Key>,
    )

    data class Profile(
        val mapper: InputMapper<List<InputActionSettingsInstance>>
    ) : NonReadingComponentProfile {
        override fun readEmpty() = OnInput(this)
    }

    class Type(private val sokol: Sokol) : ComponentType {
        override val key get() = Key

        override fun createProfile(node: ConfigurationNode): ComponentProfile {
            val engine = sokol.engine
            val mapper = node.force<InputMapper<ArrayList<InputActionSettings>>>()
            return Profile(mapper.map { settingsSet ->
                settingsSet.map { settings ->
                    fun mapClasses(names: Iterable<String>): List<KClass<out SokolComponent>> {
                        return names.map { name ->
                            @Suppress("UNCHECKED_CAST")
                            try {
                                Class.forName(name).kotlin as KClass<out SokolComponent>
                            } catch (ex: Exception) {
                                throw SerializationException(node, Profile::class.java, "Invalid component type '$name'", ex)
                            }
                        }
                    }

                    InputActionSettingsInstance(
                        engine.entityFilter(
                            mapClasses(settings.hasAll),
                            mapClasses(settings.hasOne),
                            mapClasses(settings.hasNone)
                        ),
                        settings.doKeys
                    )
                }
            })
        }
    }
}

@All(OnInput::class)
class OnInputSystem(
    private val sokol: Sokol,
    mappers: ComponentIdAccess
) : SokolSystem {
    private val mOnInput = mappers.componentMapper<OnInput>()
    private val mOnInputInstance = mappers.componentMapper<OnInputInstance>()

    @Subscribe
    fun on(event: SokolEvent.Populate, entity: SokolEntity) {
        val onInput = mOnInput.get(entity)

        val (actions) = entity.call(Build())
        mOnInputInstance.set(entity, OnInputInstance(onInput.profile.mapper.map { settingsSet ->
            settingsSet.mapNotNull { settings ->
                if (!sokol.engine.applies(settings.filter, entity))
                    return@mapNotNull null
                settings.doKeys.mapNotNull { actions[it.asString()] }
            }
        }))
    }

    data class Build(
        internal val actions: MutableMap<String, InputAction> = HashMap()
    ) : SokolEvent {
        fun addAction(key: Key, action: InputAction) {
            actions[key.asString()] = action
        }
    }
}

@All(OnInputInstance::class)
class OnInputInstanceSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mOnInputInstance = mappers.componentMapper<OnInputInstance>()

    @Subscribe
    fun on(event: PlayerInput, entity: SokolEntity) {
        val inputActionMapperInstance = mOnInputInstance.get(entity)

        val actionSets = inputActionMapperInstance.mapper.getForPlayer(event.input, event.player)
        actionSets?.forEach { actionSet ->
            for (action in actionSet) {
                if (action.run(event)) break
            }
        }
    }
}
