package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.with
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.REPLACE_MARKER
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import com.gitlab.aecsocket.sokol.paper.persistentComponent
import net.kyori.adventure.key.Key
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required
import org.spongepowered.configurate.objectmapping.meta.Setting
import kotlin.reflect.KClass

data class ItemLoreFromProfile(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("item_lore_from_profile")
        val Type = ComponentType.deserializing(Key, Profile::class)

        fun init(ctx: Sokol.InitContext) {
            ctx.persistentComponent(Type)
            ctx.system { ItemLoreFromProfileSystem(it) }
        }
    }

    override val componentType get() = ItemLoreFromProfile::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        @Required @Setting(nodeFromParent = true) val template: String
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

        val key = itemLoreFromProfile.template.replace(REPLACE_MARKER, profile.id)
        itemLoreManager.loreProvider(Lore) { i18n ->
            i18n.safe(key)
        }
    }
}
