package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable

data class ProfileItemName(val profile: Profile) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("profile_item_name")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = ProfileItemName::class
    override val key get() = Key

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()

    override fun write(node: ConfigurationNode) {}

    @ConfigSerializable
    data class Profile(
        val prefix: String = "",
        val suffix: String = ""
    ) : ComponentProfile {
        override fun read(tag: NBTTag) = ProfileItemName(this)

        override fun read(node: ConfigurationNode) = ProfileItemName(this)
    }
}

@All(ProfileItemName::class)
class ProfileItemNameSystem(engine: SokolEngine) : SokolSystem {
    private val mProfileItemName = engine.componentMapper<ProfileItemName>()

    @Subscribe
    fun on(event: SokolEvent.Populate, entity: SokolEntity) {
        val profileItemName = mProfileItemName.map(entity).profile

        val i18nKey = profileItemName.prefix + entity.profile.id + profileItemName.suffix
        entity.components.set(ItemName(i18nKey))
    }
}
