package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.input.InputMapper
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.*
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required
import org.spongepowered.configurate.objectmapping.meta.Setting

data class InputCallbacks(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("input_callbacks")
        val Type = ComponentType.deserializing(Key, Profile::class)

        fun init(ctx: Sokol.InitContext) {
            ctx.persistentComponent(Type)
            ctx.system { InputCallbacksSystem(it) }
        }
    }

    @ConfigSerializable
    data class CallbackData(
        @Required @Setting(value = "do") val mDo: MutableList<List<EntityCallback>>,
        @Setting(value = "if") val mIf: Set<String> = emptySet()
    )

    override val componentType get() = InputCallbacks::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        @Required @Setting(nodeFromParent = true) val inputs: InputMapper<CallbackData>
    ) : SimpleComponentProfile<InputCallbacks> {
        override val componentType get() = InputCallbacks::class

        override fun createEmpty() = ComponentBlueprint { InputCallbacks(this) }
    }
}

@All(InputCallbacks::class)
class InputCallbacksSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mInputCallbacks = ids.mapper<InputCallbacks>()

    @Subscribe
    fun on(event: PlayerInputEvent, entity: SokolEntity) {
        val inputCallbacks = mInputCallbacks.get(entity).profile

        val player = event.player
        val conditions = setOf(
            if (player.isSneaking) "player_sneaking" else "player_not_sneaking",
            if (player.isSprinting) "player_sprinting" else "player_not_sprinting",
            if (player.isFlying) "player_flying" else "player_not_flying"
        )

        inputCallbacks.inputs.matches(event.input).forEach { (mDo, mIf) ->
            if (!conditions.containsAll(mIf)) return@forEach
            mDo.forEach { callbackSet ->
                for (callback in callbackSet) {
                    val result = try {
                        callback.action.run(entity, event.player)
                    } catch (ex: Exception) {
                        throw RuntimeException("Could not run input callback ${callback.key}", ex)
                    }
                    if (result) {
                        event.cancel()
                        break
                    }
                }
            }
        }
    }
}