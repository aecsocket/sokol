package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.AlexandriaAPI
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.glossa.core.force
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.PersistentComponent
import com.gitlab.aecsocket.sokol.paper.PersistentComponentFactory
import com.gitlab.aecsocket.sokol.paper.PersistentComponentType
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.spongepowered.configurate.ConfigurationNode

data class ItemName(
    val i18nKey: String
) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("item_name")
    }

    override val componentType get() = ItemName::class.java
    override val key get() = Key

    override fun write(ctx: NBTTagContext) = ctx.makeString(i18nKey)

    override fun write(node: ConfigurationNode) {
        node.set(i18nKey)
    }

    object Type : PersistentComponentType {
        override val componentType get() = ItemName::class.java
        override val key get() = Key

        override fun read(tag: NBTTag) = ItemName(tag.asString())

        override fun read(node: ConfigurationNode) = ItemName(node.force())

        override fun readFactory(node: ConfigurationNode): PersistentComponentFactory {
            val i18nKey = node.force<String>()
            return PersistentComponentFactory { ItemName(i18nKey) }
        }
    }
}

@All(ItemName::class, HostedByItem::class)
class ItemNameSystem(engine: SokolEngine) : SokolSystem {
    private val mItemName = engine.componentMapper<ItemName>()
    private val mItem = engine.componentMapper<HostedByItem>()
    private val mItemHolder = engine.componentMapper<ItemHolder>()

    @Subscribe
    fun on(event: SokolEvent.Add, entity: SokolEntityAccess) {
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
