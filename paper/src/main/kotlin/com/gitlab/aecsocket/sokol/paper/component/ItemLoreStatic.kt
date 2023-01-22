package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.with
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.glossa.core.I18N
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import com.gitlab.aecsocket.sokol.paper.persistentComponent
import net.kyori.adventure.text.Component
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required
import org.spongepowered.configurate.objectmapping.meta.Setting

data class ItemLoreStatic(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("item_lore_static")
        val Type = ComponentType.deserializing(Key, Profile::class)

        fun init(ctx: Sokol.InitContext) {
            ctx.persistentComponent(Type)
            ctx.system { ItemLoreStaticSystem(it).init(ctx) }
        }
    }

    override val componentType get() = ItemLoreStatic::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        @Required @Setting(nodeFromParent = true) val i18nKey: String
    ) : SimpleComponentProfile<ItemLoreStatic> {
        override val componentType get() = ItemLoreStatic::class

        override fun createEmpty() = ComponentBlueprint { ItemLoreStatic(this) }
    }
}

class ItemLoreStaticSystem(ids: ComponentIdAccess) : SokolSystem {
    companion object {
        val Lore = ItemLoreStatic.Key.with("lore")
    }

    private val mItemLoreStatic = ids.mapper<ItemLoreStatic>()

    internal fun init(ctx: Sokol.InitContext): ItemLoreStaticSystem {
        ctx.components.itemLoreManager.apply {
            provider(Lore, ::lore)
        }
        return this
    }

    private fun lore(entity: SokolEntity, i18n: I18N<Component>): List<Component> {
        val itemLoreStatic = mItemLoreStatic.getOr(entity)?.profile ?: return emptyList()

        return i18n.safe(itemLoreStatic.i18nKey)
    }
}
