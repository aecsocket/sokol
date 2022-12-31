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
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type
import kotlin.reflect.KClass

private const val FORMATTER = "formatter"
private const val FORMATTERS = "formatters"
private const val TABLE_ALIGNS = "table_aligns"
private const val COLUMN_SEPARATOR_KEY = "column_separator_key"

interface StatFormatter<V : Any> {
    val stat: Stat<V>
    val nameKey: String

    fun format(i18n: I18N<Component>, value: StatValue<V>): TableRow<Component>
}

class StatFormatterSerializer(private val sokol: Sokol) : TypeSerializer<StatFormatter<*>> {
    override fun serialize(type: Type, obj: StatFormatter<*>?, node: ConfigurationNode) {}

    override fun deserialize(type: Type, node: ConfigurationNode): StatFormatter<*> {
        val key = node.node(FORMATTER).force<Key>()
        val formatterType = sokol.components.itemLoreStats.formatterType(key)
            ?: throw SerializationException(node, type, "Invalid formatter type '$key'")
        return node.force(formatterType)
    }
}

data class ItemLoreStats(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("item_lore_stats")
    }

    override val componentType get() = ItemLoreStats::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        val formatters: List<StatFormatter<*>> = emptyList(),
        val tableAligns: TableAligns = TableAligns.Default,
        val columnSeparatorKey: String? = null,
        val lineKey: String? = null,
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

@All(ItemLoreStats::class, ItemLoreManager::class, Stats::class)
@Before(ItemLoreManagerSystem::class)
class ItemLoreStatsSystem(ids: ComponentIdAccess) : SokolSystem {
    companion object {
        val Lore = ItemLoreStats.Key.with("lore")
    }

    private val mItemLoreStats = ids.mapper<ItemLoreStats>()
    private val mItemLoreManager = ids.mapper<ItemLoreManager>()
    private val mStatsInstance = ids.mapper<StatsInstance>()

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        val itemLoreStats = mItemLoreStats.get(entity).profile
        val itemLoreManager = mItemLoreManager.get(entity)

        itemLoreManager.loreProvider(Lore) { i18n ->
            val stats = mStatsInstance.statMap(entity)

            val columnSeparator = itemLoreStats.columnSeparatorKey?.let { i18n.makeOne(it) } ?: Component.empty()

            val renderer = AlexandriaAPI.ComponentTableRenderer(
                align = itemLoreStats.tableAligns.aligner(),
                justify = itemLoreStats.tableAligns.justifier(),
                colSeparator = columnSeparator,
                rowSeparator = { emptyList() }
            )

            fun <V : Any> format(formatter: StatFormatter<V>, value: StatValue<*>): TableRow<Component> {
                @Suppress("UNCHECKED_CAST")
                return formatter.format(i18n, value as StatValue<V>)
            }

            val rows: TableRow<TableCell<Component>> = itemLoreStats.formatters.mapNotNull { formatter ->
                val value = stats[formatter.stat] ?: return@mapNotNull null
                val col1: List<Component> = i18n.safe(formatter.nameKey)
                val valueCols: TableRow<Component> = format(formatter, value)
                listOf(col1) + valueCols
            }

            renderer.render(rows).flatMap { line ->
                itemLoreStats.lineKey?.let { i18n.make(it) {
                    subst("line", line)
                } } ?: listOf(line)
            }
        }
    }
}
