package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.TableAligns
import com.gitlab.aecsocket.alexandria.core.TableCell
import com.gitlab.aecsocket.alexandria.core.TableRow
import com.gitlab.aecsocket.alexandria.core.extension.force
import com.gitlab.aecsocket.alexandria.core.extension.with
import com.gitlab.aecsocket.alexandria.paper.AlexandriaAPI
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.glossa.core.I18N
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import com.gitlab.aecsocket.sokol.paper.persistentComponent
import com.gitlab.aecsocket.sokol.paper.stat.NameStatFormatter
import com.gitlab.aecsocket.sokol.paper.stat.NumberStatBarFormatter
import com.gitlab.aecsocket.sokol.paper.stat.NumberStatFormatter
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type
import kotlin.reflect.KClass

private const val TYPE = "type"

fun interface StatFormatter<V : Any> {
    fun format(i18n: I18N<Component>, value: StatValue<V>): Iterable<TableCell<Component>>
}

class StatFormatterSerializer(private val itemLoreStats: ItemLoreStats.Type) : TypeSerializer<StatFormatter<*>> {
    override fun serialize(type: Type, obj: StatFormatter<*>?, node: ConfigurationNode) {}

    override fun deserialize(type: Type, node: ConfigurationNode): StatFormatter<*> {
        val key = node.node(TYPE).force<Key>()
        val formatterType = itemLoreStats.formatterType(key)
            ?: throw SerializationException(node, type, "Invalid formatter type '$key'")
        return node.force(formatterType)
    }
}

data class ItemLoreStats(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("item_lore_stats")

        fun init(ctx: Sokol.InitContext) {
            val sokol = ctx.sokol
            val component = sokol.components.itemLoreStats
            ctx.persistentComponent(component)
            ctx.system { ItemLoreStatsSystem(it).init(ctx) }

            component.formatterType<NameStatFormatter>(sokol.key("name"))
            component.formatterType<NumberStatFormatter>(sokol.key("number"))
            component.formatterType<NumberStatBarFormatter>(sokol.key("number_bar"))
        }
    }

    @ConfigSerializable
    data class StatRow(
        val stat: Stat<*>,
        val formatters: List<StatFormatter<*>>
    )

    override val componentType get() = ItemLoreStats::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        @Required val baseKey: String,
        @Required val order: List<StatRow>,
        val tableAligns: TableAligns = TableAligns.Default
    ) : SimpleComponentProfile<ItemLoreStats> {
        override val componentType get() = ItemLoreStats::class

        override fun createEmpty() = ComponentBlueprint { ItemLoreStats(this) }
    }

    class Type : ComponentType<ItemLoreStats> {
        override val key get() = Key

        private val _formatterTypes = HashMap<String, KClass<out StatFormatter<*>>>()
        val formatterTypes: Map<String, KClass<out StatFormatter<*>>> get() = _formatterTypes

        fun formatterType(key: Key) = _formatterTypes[key.asString()]
        fun formatterType(key: Key, type: KClass<out StatFormatter<*>>) {
            if (_formatterTypes.contains(key.asString()))
                throw IllegalArgumentException("Duplicate stat formatter type $key")
            _formatterTypes[key.asString()] = type
        }

        override fun createProfile(node: ConfigurationNode) = node.force<Profile>()
    }
}

inline fun <reified F : StatFormatter<*>> ItemLoreStats.Type.formatterType(key: Key) = formatterType(key, F::class)

private const val COLUMN_SEPARATOR = "column_separator"

class ItemLoreStatsSystem(ids: ComponentIdAccess) : SokolSystem {
    companion object {
        val Lore = ItemLoreStats.Key.with("lore")
    }

    private val mItemLoreStats = ids.mapper<ItemLoreStats>()
    private val mStatsInstance = ids.mapper<StatsInstance>()

    private fun ItemLoreStats.Profile.key(path: String) = "$baseKey.$path"

    internal fun init(ctx: Sokol.InitContext): ItemLoreStatsSystem {
        ctx.components.itemLoreManager.apply {
            provider(Lore, ::lore)
        }
        return this
    }

    private fun lore(entity: SokolEntity, i18n: I18N<Component>): List<Component> {
        val itemLoreStats = mItemLoreStats.getOr(entity)?.profile ?: return emptyList()

        val stats = mStatsInstance.statMap(entity)

        fun <V : Any> format(formatter: StatFormatter<V>, value: StatValue<*>): Iterable<TableCell<Component>> {
            @Suppress("UNCHECKED_CAST")
            return formatter.format(i18n, value as StatValue<V>)
        }

        val rows: List<TableRow<Component>> = itemLoreStats.order.mapNotNull { row ->
            val value = stats[row.stat] ?: return@mapNotNull null
            val cols = row.formatters.flatMap { format(it, value) }
            TableRow(cols)
        }

        val columnSeparator = i18n.makeOne(itemLoreStats.key(COLUMN_SEPARATOR)) ?: Component.empty()
        return AlexandriaAPI.ComponentTableRenderer(
            align = itemLoreStats.tableAligns.aligner(),
            justify = itemLoreStats.tableAligns.justifier(),
            colSeparator = columnSeparator,
            rowSeparator = { emptyList() }
        ).render(rows)
    }
}
