package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.input.InputMapper
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.PlayerInputEvent
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import net.kyori.adventure.key.Key
import org.bukkit.entity.Player
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

fun interface InputCallback {
    fun run(player: Player): Boolean
}

data class InputCallbacks(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("input_callbacks")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    @ConfigSerializable
    data class Callback(
        @Setting(value = "if") val mIf: Set<String>,
        @Setting(value = "do") val mDo: MutableList<List<Key>> // MutableList so it deserializes as List<Key> not List<? extends Key>
    )

    override val componentType get() = InputCallbacks::class
    override val key get() = Key

    private val _callbacks = HashMap<Key, InputCallback>()
    val callbacks: Map<Key, InputCallback> get() = _callbacks

    fun callback(key: Key) = _callbacks[key]

    fun callback(key: Key, callback: InputCallback) {
        if (_callbacks.contains(key))
            throw IllegalArgumentException("Duplicate input callback $key")
        _callbacks[key] = callback
    }

    @ConfigSerializable
    data class Profile(
        @Setting(nodeFromParent = true) val inputs: InputMapper<Callback>
    ) : SimpleComponentProfile {
        override val componentType get() = InputCallbacks::class

        override fun createEmpty() = ComponentBlueprint { InputCallbacks(this) }
    }
}

data class InputCallbacksInstance(
    val inputs: InputMapper<Callback>
) : SokolComponent {
    override val componentType get() = InputCallbacksInstance::class

    data class Callback(
        val mIf: Set<String>,
        val mDo: List<List<InputCallback>>
    )
}

object InputCallbacksInstanceTarget : SokolSystem

@All(InputCallbacks::class)
@None(InputCallbacksInstance::class)
@Before(InputCallbacksInstanceTarget::class)
class InputCallbacksSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mInputCallbacks = ids.mapper<InputCallbacks>()
    private val mInputCallbacksInstance = ids.mapper<InputCallbacksInstance>()

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        val inputCallbacks = mInputCallbacks.get(entity)

        val inputs = inputCallbacks.profile.inputs.map { (mIf, mDo) ->
            InputCallbacksInstance.Callback(mIf, mDo.map { a -> a.mapNotNull { b -> inputCallbacks.callback(b) } })
        }
        mInputCallbacksInstance.set(entity, InputCallbacksInstance(inputs))
    }
}

@All(InputCallbacksInstance::class)
@After(InputCallbacksInstanceTarget::class)
class InputCallbacksInstanceSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mInputCallbacksInstance = ids.mapper<InputCallbacksInstance>()

    @Subscribe
    fun on(event: PlayerInputEvent, entity: SokolEntity) {
        val inputCallbacksInstance = mInputCallbacksInstance.get(entity)

        val player = event.player
        val conditions = setOf(
            if (player.isSneaking) "player_sneaking" else "player_not_sneaking",
            if (player.isSprinting) "player_sprinting" else "player_not_sprinting",
            if (player.isFlying) "player_flying" else "player_not_flying"
        )

        inputCallbacksInstance.inputs.matches(event.input).forEach { (mIf, mDo) ->
            if (!conditions.containsAll(mIf)) return@forEach
            mDo.forEach { callbackSet ->
                for (callback in callbackSet) {
                    if (callback.run(event.player)) {
                        event.cancel()
                        break
                    }
                }
            }
        }
    }
}
