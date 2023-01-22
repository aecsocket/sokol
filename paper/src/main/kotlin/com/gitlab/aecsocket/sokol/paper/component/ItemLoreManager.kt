package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.force
import com.gitlab.aecsocket.alexandria.core.extension.join
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.glossa.core.I18N
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.ItemEvent
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import com.gitlab.aecsocket.sokol.paper.persistentComponent
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

fun interface LoreProvider {
    fun create(entity: SokolEntity, i18n: I18N<Component>): List<Component>
}

data class LoreProviderData(
    val key: Key,
    val provider: LoreProvider
)

class LoreProviderSerializer(private val itemLoreManager: ItemLoreManager.Type) : TypeSerializer<LoreProviderData> {
    override fun serialize(type: Type, obj: LoreProviderData?, node: ConfigurationNode) {}

    override fun deserialize(type: Type, node: ConfigurationNode): LoreProviderData {
        val key = node.force<Key>()
        return itemLoreManager.provider(key)
            ?: throw SerializationException(node, type, "Invalid lore provider '$key'")
    }
}

private val unstyled = Component.text("").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)

data class ItemLoreManager(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("item_lore_manager")
        val Type = ComponentType.deserializing(Key, Profile::class)

        fun init(ctx: Sokol.InitContext) {
            ctx.persistentComponent(ctx.components.itemLoreManager)
            ctx.system { ItemLoreManagerSystem(it) }
        }
    }

    override val componentType get() = ItemLoreManager::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        @Required val order: List<LoreProviderData>,
        val separatorKey: String? = null,
        val lineKey: String? = null
    ) : SimpleComponentProfile<ItemLoreManager> {
        override val componentType get() = ItemLoreManager::class

        override fun createEmpty() = ComponentBlueprint { ItemLoreManager(this) }
    }

    class Type : ComponentType<ItemLoreManager> {
        override val key get() = Key

        private val _providers = HashMap<Key, LoreProviderData>()
        val providers: Map<Key, LoreProviderData> get() = _providers

        fun provider(key: Key) = _providers[key]

        fun provider(key: Key, provider: LoreProvider) {
            if (_providers.contains(key))
                throw IllegalArgumentException("Duplicate lore provider $key")
            _providers[key] = LoreProviderData(key, provider)
        }

        override fun createProfile(node: ConfigurationNode) = node.force<Profile>()
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
        val lore = itemLoreManager.profile.order.mapNotNull { provider ->
            val lore = provider.provider.create(entity, i18n)
            lore.ifEmpty { null } // empty sections won't add another separator
        }.join(separator)

        isItem.writeMeta { meta ->
            meta.lore(lore.flatMap { line ->
                (itemLoreManager.profile.lineKey?.let { i18n.make(it) {
                    subst("line", line)
                } } ?: listOf(line)).map {
                    unstyled.append(it)
                }
            })
        }
    }
}
