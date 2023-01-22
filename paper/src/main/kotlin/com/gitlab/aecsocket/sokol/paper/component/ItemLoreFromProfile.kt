package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.with
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.glossa.core.I18N
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.REPLACE_MARKER
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import com.gitlab.aecsocket.sokol.paper.persistentComponent
import net.kyori.adventure.text.Component
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required
import org.spongepowered.configurate.objectmapping.meta.Setting

data class ItemLoreFromProfile(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("item_lore_from_profile")
        val Type = ComponentType.deserializing(Key, Profile::class)

        fun init(ctx: Sokol.InitContext) {
            ctx.persistentComponent(Type)
            ctx.system { ItemLoreFromProfileSystem(it).init(ctx) }
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

@Before(ItemLoreManagerSystem::class)
class ItemLoreFromProfileSystem(ids: ComponentIdAccess) : SokolSystem {
    companion object {
        val Lore = ItemLoreFromProfile.Key.with("lore")
    }

    private val mItemLoreFromProfile = ids.mapper<ItemLoreFromProfile>()
    private val mProfiled = ids.mapper<Profiled>()

    internal fun init(ctx: Sokol.InitContext): ItemLoreFromProfileSystem {
        ctx.components.itemLoreManager.apply {
            provider(Lore, ::lore)
        }
        return this
    }

    private fun lore(entity: SokolEntity, i18n: I18N<Component>): List<Component> {
        val itemLoreFromProfile = mItemLoreFromProfile.getOr(entity)?.profile ?: return emptyList()
        val profile = mProfiled.getOr(entity)?.profile ?: return emptyList()

        val key = itemLoreFromProfile.template.replace(REPLACE_MARKER, profile.id)
        return i18n.safe(key)
    }
}
