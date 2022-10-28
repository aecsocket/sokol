package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required

data class ItemNameStatic(val profile: Profile) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("item_name_static")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = ItemNameStatic::class
    override val key get() = Key

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()

    override fun write(node: ConfigurationNode) {}

    @ConfigSerializable
    data class Profile(
        @Required val i18nKey: String
    ) : NonReadingComponentProfile {
        override fun readEmpty() = ItemNameStatic(this)
    }
}

@All(ItemNameStatic::class)
@Before(ItemNameSystem::class)
class ItemNameStaticSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mItemNameStatic = mappers.componentMapper<ItemNameStatic>()
    private val mItemName = mappers.componentMapper<ItemName>()

    @Subscribe
    fun on(event: SokolEvent.Populate, entity: SokolEntity) {
        val staticItemName = mItemNameStatic.get(entity).profile

        mItemName.set(entity, ItemName(staticItemName.i18nKey))
    }
}
