package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.AlexandriaAPI
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.glossa.core.force
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.TextDecoration
import org.spongepowered.configurate.ConfigurationNode

data class ItemName(
    val i18nKey: String
) : SokolComponent {
    override val componentType get() = ItemName::class
}

@All(ItemName::class, HostedByItem::class)
class ItemNameSystem(engine: SokolEngine) : SokolSystem {
    private val mItemName = engine.componentMapper<ItemName>()
    private val mItem = engine.componentMapper<HostedByItem>()
    private val mItemHolder = engine.componentMapper<ItemHolder>()

    @Subscribe
    fun on(event: SokolEvent.Add, entity: SokolEntity) {
        val itemName = mItemName.map(entity)
        val item = mItem.map(entity)
        val itemHolder = mItemHolder.mapOr(entity)

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
