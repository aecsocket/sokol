package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.join
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.glossa.core.I18N
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.ItemEvent
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.spongepowered.configurate.objectmapping.ConfigSerializable

typealias LoreProvider = (I18N<Component>) -> List<Component>

data class ItemLoreManager(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("item_lore_manager")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = ItemLoreManager::class
    override val key get() = Key

    private val _loreProviders = HashMap<Key, LoreProvider>()
    val loreProviders: Map<Key, LoreProvider> get() = _loreProviders

    fun loreProvider(key: Key) = _loreProviders[key]

    fun loreProvider(key: Key, provider: LoreProvider) {
        if (_loreProviders.containsKey(key))
            throw IllegalArgumentException("Duplicate lore provider $key")
        _loreProviders[key] = provider
    }

    @ConfigSerializable
    data class Profile(
        val order: List<Key> = emptyList(),
        val separatorKey: String? = null
    ) : SimpleComponentProfile {
        override val componentType get() = ItemLoreManager::class

        override fun createEmpty() = ComponentBlueprint { ItemLoreManager(this) }
    }
}

@All(ItemLoreManager::class, IsItem::class)
class ItemLoreManagerSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mItemLoreManager = ids.mapper<ItemLoreManager>()
    private val mIsItem = ids.mapper<IsItem>()
    private val mItemHolder = ids.mapper<ItemHolder>()

    @Subscribe
    fun on(event: ItemEvent.Create, entity: SokolEntity) {
        val itemLoreManager = mItemLoreManager.get(entity)
        val isItem = mIsItem.get(entity)
        val i18n = mItemHolder.i18n(entity)

        val separator = itemLoreManager.profile.separatorKey?.let { i18n.make(it) } ?: emptyList()
        val lore = itemLoreManager.profile.order.mapNotNull { key ->
            val loreProvider = itemLoreManager.loreProvider(key) ?: return@mapNotNull null
            loreProvider(i18n)
        }.join(separator)

        isItem.writeMeta { meta ->
            meta.lore(lore)
        }
    }
}
