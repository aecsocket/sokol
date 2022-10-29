package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.*
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable

data class EntitySlot(val profile: Profile) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("entity_slot")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = EntitySlot::class
    override val key get() = Key

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()

    override fun write(node: ConfigurationNode) {}

    @ConfigSerializable
    data class Profile(
        val accepts: Boolean
    ) : NonReadingComponentProfile {
        override fun readEmpty() = EntitySlot(this)
    }
}
