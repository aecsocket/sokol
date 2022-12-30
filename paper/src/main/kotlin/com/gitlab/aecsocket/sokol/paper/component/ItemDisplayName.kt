package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.ItemEvent
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration

object ItemDisplayName : SimplePersistentComponent {
    override val componentType get() = ItemDisplayName::class
    override val key = SokolAPI.key("item_display_name")
    val Type = ComponentType.singletonComponent(key, this)
}

@All(ItemDisplayName::class, DisplayName::class, IsItem::class)
@After(DisplayNameTarget::class)
class ItemDisplayNameSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mDisplayName = ids.mapper<DisplayName>()
    private val mIsItem = ids.mapper<IsItem>()
    private val mItemHolder = ids.mapper<ItemHolder>()

    @Subscribe
    fun on(event: ItemEvent.Create, entity: SokolEntity) {
        val displayName = mDisplayName.get(entity)
        val isItem = mIsItem.get(entity)
        val i18n = mItemHolder.i18n(entity)

        isItem.writeMeta { meta ->
            meta.displayName(Component.empty()
                .decoration(TextDecoration.ITALIC, false)
                .append(displayName.nameFor(i18n)))
        }
    }
}
