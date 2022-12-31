package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.with
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import net.kyori.adventure.key.Key
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting
import kotlin.reflect.KClass

data class ItemLoreFromProfile(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("item_lore_from_profile")
        val Type = ComponentType.deserializing(Key, Profile::class)
    }

    override val componentType get() = ItemLoreFromProfile::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        val prefix: String = "",
        val suffix: String = ""
    ) : SimpleComponentProfile<ItemLoreFromProfile> {
        override val componentType get() = ItemLoreFromProfile::class

        override fun createEmpty() = ComponentBlueprint { ItemLoreFromProfile(this) }
    }
}

@All(ItemLoreFromProfile::class, ItemLoreManager::class, Profiled::class)
@Before(ItemLoreManagerSystem::class)
class ItemLoreFromProfileSystem(ids: ComponentIdAccess) : SokolSystem {
    companion object {
        val Lore = ItemLoreFromProfile.Key.with("lore")
    }

    private val mItemLoreFromProfile = ids.mapper<ItemLoreFromProfile>()
    private val mItemLoreManager = ids.mapper<ItemLoreManager>()
    private val mProfiled = ids.mapper<Profiled>()

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        val itemLoreFromProfile = mItemLoreFromProfile.get(entity).profile
        val itemLoreManager = mItemLoreManager.get(entity)
        val profile = mProfiled.get(entity).profile

        itemLoreManager.loreProvider(Lore) { i18n ->
            i18n.safe(itemLoreFromProfile.prefix + profile.id + itemLoreFromProfile.suffix)
        }
    }
}
