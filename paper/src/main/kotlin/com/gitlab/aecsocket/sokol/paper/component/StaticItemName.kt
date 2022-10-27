package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required

data class StaticItemName(val profile: Profile) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("static_item_name")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = StaticItemName::class
    override val key get() = Key

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()

    override fun write(node: ConfigurationNode) {}

    @ConfigSerializable
    data class Profile(
        @Required val i18nKey: String
    ) : ComponentProfile {
        override fun read(tag: NBTTag) = StaticItemName(this)

        override fun read(node: ConfigurationNode) = StaticItemName(this)
    }
}

@All(StaticItemName::class)
class StaticItemNameSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mStaticItemName = mappers.componentMapper<StaticItemName>()

    @Subscribe
    fun on(event: SokolEvent.Populate, entity: SokolEntity) {
        val staticItemName = mStaticItemName.map(entity).profile

        entity.components.set(ItemName(staticItemName.i18nKey))
    }
}
