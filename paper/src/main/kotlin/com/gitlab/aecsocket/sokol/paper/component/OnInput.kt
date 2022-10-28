package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.input.InputMapper
import com.gitlab.aecsocket.alexandria.paper.extension.getForPlayer
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.PlayerInput
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import net.kyori.adventure.key.Key
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

fun interface InputAction {
    fun run(event: PlayerInput)
}

data class OnInputInstance(
    var mapper: InputMapper<List<InputAction>>
) : SokolComponent {
    override val componentType get() = OnInputInstance::class
}

data class OnInput(val profile: Profile) : PersistentComponent {
    companion object {
        const val HOSTED_BY_ITEM = "hosted_by_item"
        const val LOOKED_AT = "looked_at"

        val Key = SokolAPI.key("on_input")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = OnInput::class
    override val key get() = Key

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()

    override fun write(node: ConfigurationNode) {}

    @ConfigSerializable
    data class Profile(
        @Setting(nodeFromParent = true) val mapper: InputMapper<List<Key>>
    ) : NonReadingComponentProfile {
        override fun readEmpty() = OnInput(this)
    }
}

@All(OnInput::class)
class OnInputSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mOnInput = mappers.componentMapper<OnInput>()
    private val mOnInputInstance = mappers.componentMapper<OnInputInstance>()

    @Subscribe
    fun on(event: SokolEvent.Populate, entity: SokolEntity) {
        val onInput = mOnInput.get(entity)

        val (actions) = entity.call(Build())
        mOnInputInstance.set(entity, OnInputInstance(onInput.profile.mapper.map { keys ->
            keys.mapNotNull { actions[it.asString()] }
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
    private val mItem = mappers.componentMapper<HostedByItem>()
    private val mLookedAt = mappers.componentMapper<LookedAt>()

    @Subscribe
    fun on(event: PlayerInput, entity: SokolEntity) {
        val inputActionMapperInstance = mOnInputInstance.get(entity)

        val tags = HashSet<String>()
        if (mItem.has(entity)) tags.add(OnInput.HOSTED_BY_ITEM)
        if (mLookedAt.has(entity)) tags.add(OnInput.LOOKED_AT)

        val actions = inputActionMapperInstance.mapper.getForPlayer(event.input, event.player, tags)
        actions?.forEach { action ->
            action.run(event)
        }
    }
}
