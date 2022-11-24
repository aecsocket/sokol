package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Shape
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required

data class EntitySlot(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("entity_slot")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = EntitySlot::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        @Required val shape: Shape,
        val allows: Boolean = true
    ) : SimpleComponentProfile {
        override val componentType get() = EntitySlot::class

        override fun createEmpty() = ComponentBlueprint { EntitySlot(this) }
    }
}
