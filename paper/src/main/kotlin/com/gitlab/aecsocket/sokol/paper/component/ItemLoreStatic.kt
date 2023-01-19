package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.with
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import com.gitlab.aecsocket.sokol.paper.persistentComponent
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

data class ItemLoreStatic(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("item_lore_static")
        val Type = ComponentType.deserializing(Key, Profile::class)

        fun init(ctx: Sokol.InitContext) {
            ctx.persistentComponent(Type)
            ctx.system { ItemLoreStaticSystem(it) }
        }
    }

    override val componentType get() = ItemLoreStatic::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        @Setting(nodeFromParent = true) val i18nKey: String
    ) : SimpleComponentProfile<ItemLoreStatic> {
        override val componentType get() = ItemLoreStatic::class

        override fun createEmpty() = ComponentBlueprint { ItemLoreStatic(this) }
    }
}

@All(ItemLoreStatic::class, ItemLoreManager::class)
@Before(ItemLoreManagerSystem::class)
class ItemLoreStaticSystem(ids: ComponentIdAccess) : SokolSystem {
    companion object {
        val Lore = ItemLoreStatic.Key.with("lore")
    }

    private val mItemLoreStatic = ids.mapper<ItemLoreStatic>()
    private val mItemLoreManager = ids.mapper<ItemLoreManager>()

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        val itemLoreStatic = mItemLoreStatic.get(entity).profile
        val itemLoreManager = mItemLoreManager.get(entity)

        itemLoreManager.loreProvider(Lore) { i18n ->
            i18n.safe(itemLoreStatic.i18nKey)
        }
    }
}
