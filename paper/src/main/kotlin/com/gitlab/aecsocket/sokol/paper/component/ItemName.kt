package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.AlexandriaAPI
import com.gitlab.aecsocket.sokol.core.*
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.TextDecoration

data class ItemName(
    val i18nKey: String
) : SokolComponent {
    override val componentType get() = ItemName::class
}

@All(ItemName::class, HostedByItem::class)
@After(HostedByItemFormTarget::class)
class ItemNameSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mItemName = mappers.componentMapper<ItemName>()
    private val mItem = mappers.componentMapper<HostedByItem>()
    private val mItemHolder = mappers.componentMapper<ItemHolder>()

    @Subscribe
    fun on(event: SokolEvent.Add, entity: SokolEntity) {
        val itemName = mItemName.get(entity)
        val item = mItem.get(entity)
        val itemHolder = mItemHolder.getOr(entity)

        val i18n = if (itemHolder is ItemHolder.ByMob) AlexandriaAPI.i18nFor(itemHolder.mob) else AlexandriaAPI.i18n
        val name = i18n.safeOne(itemName.i18nKey)

        item.writeMeta { meta ->
            meta.displayName(text()
                .decoration(TextDecoration.ITALIC, false)
                .append(name)
                .build()
            )
        }
    }
}
