package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.keyed.Keyed
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import org.spongepowered.configurate.objectmapping.ConfigSerializable

data class ItemNameProfile(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("item_name_profile")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = ItemNameProfile::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        val prefix: String = "",
        val suffix: String = ""
    ) : SimpleComponentProfile {
        override fun readEmpty() = ItemNameProfile(this)
    }
}

@All(ItemNameProfile::class)
@Before(ItemNameSystem::class)
class ItemNameProfileSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mItemNameProfile = mappers.componentMapper<ItemNameProfile>()
    private val mItemName = mappers.componentMapper<ItemName>()

    @Subscribe
    fun on(event: SokolEvent.Populate, entity: SokolEntity) {
        val profileItemName = mItemNameProfile.get(entity).profile

        val profile = entity.profile
        if (profile !is Keyed)
            throw SystemExecutionException("Profile must be keyed")

        val i18nKey = profileItemName.prefix + profile.id + profileItemName.suffix
        mItemName.set(entity, ItemName(i18nKey))
    }
}
