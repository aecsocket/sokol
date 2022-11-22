package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import net.kyori.adventure.key.Key
import org.spongepowered.configurate.objectmapping.ConfigSerializable

data class InputActions(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("input_actions")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = InputActions::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        // todo
        val a: Boolean = false
    ) : SimpleComponentProfile {
        override val componentType get() = InputActions::class

        override fun createEmpty() = ComponentBlueprint { InputActions(this) }
    }
}
