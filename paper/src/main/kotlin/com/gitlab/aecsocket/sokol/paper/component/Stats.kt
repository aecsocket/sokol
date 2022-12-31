package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.force
import com.gitlab.aecsocket.alexandria.core.extension.forceList
import com.gitlab.aecsocket.alexandria.core.keyed.parseNodeNamespacedKey
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import net.kyori.adventure.key.Key
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type
import kotlin.reflect.KClass

interface Stat<V : Any> {
    val key: Key

    fun createNode(node: ConfigurationNode): StatNode<V>
}

interface StatNode<V : Any> {
    fun with(last: V): V

    interface First<V : Any> : StatNode<V> {
        fun first(): V
    }
}

class StatValue<V : Any>(
    val stat: Stat<V>,
    nodes: MutableList<StatNode<V>>
) : MutableList<StatNode<V>> by nodes {
    fun computeOr(): V? {
        var current: V? = null
        forEach {
            val nCurrent = current
            if (nCurrent == null) {
                current = (it as? StatNode.First<V> ?: return null).first()
            } else {
                current = it.with(nCurrent)
            }
        }
        return current
    }

    fun compute() = computeOr()
        ?: throw IllegalStateException("Stat requires a first-node to compute value")
}

class StatMap(map: MutableMap<Stat<*>, StatValue<*>>) : MutableMap<Stat<*>, StatValue<*>> by map {
    fun <V : Any> value(stat: Stat<V>): V {
        @Suppress("UNCHECKED_CAST")
        val value = get(stat) as StatValue<V>
        return value.compute()
    }

    fun <V : Any> combine(value: StatValue<V>) {
        val stat = value.stat
        get(stat)?.let { existing ->
            @Suppress("UNCHECKED_CAST")
            (existing as StatValue<V>).addAll(value)
        } ?: set(stat, value)
    }

    fun combine(other: Iterable<StatValue<*>>) {
        other.forEach { combine(it) }
    }

    fun combine(other: Map<Stat<*>, StatValue<*>>) {
        other.forEach { (_, value) -> combine(value) }
    }
}

class StatOperationSerializer<V : Any>(
    private val valueType: KClass<V>,
    private val operations: Map<String, (V) -> StatNode<V>>
) {
    fun createValue(node: ConfigurationNode): StatNode<V> {
        val list = node.forceList(valueType.java, "operation", "value")
        val opKey = list[0].force<String>()
        val operation = operations[opKey]
            ?: throw SerializationException(node, valueType.java, "Invalid operation '$opKey'")
        val value = list[1].force(valueType)
        return operation(value)
    }
}

class StatSerializer(private val sokol: Sokol) : TypeSerializer<Stat<*>> {
    override fun serialize(type: Type, obj: Stat<*>?, node: ConfigurationNode) {}

    override fun deserialize(type: Type, node: ConfigurationNode): Stat<*> {
        val key = node.force<Key>()
        return sokol.components.stats.stat(key)
            ?: throw SerializationException(node, type, "Invalid stat '$key'")
    }
}

data class Stats(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("stats")
    }

    override val componentType get() = Stats::class
    override val key get() = Key

    data class Profile(
        val stats: StatMap
    ) : SimpleComponentProfile<Stats> {
        override val componentType get() = Stats::class

        override fun createEmpty() = ComponentBlueprint { Stats(this) }
    }

    class Type : ComponentType<Stats> {
        override val key get() = Key

        private val _stats = HashMap<Key, Stat<*>>()
        val stats: Map<Key, Stat<*>> get() = _stats

        fun stat(key: Key) = _stats[key]

        fun stat(stat: Stat<*>) {
            val key = stat.key
            if (_stats.contains(key))
                throw IllegalArgumentException("Duplicate stat $key")
            _stats[key] = stat
        }

        fun stats(stats: Iterable<Stat<*>>) {
            stats.forEach { stat(it) }
        }

        override fun createProfile(node: ConfigurationNode): ComponentProfile<Stats> {
            val type = Profile::class.java

            fun <V : Any> createStat(stat: Stat<V>, child: ConfigurationNode): Pair<Stat<V>, StatValue<V>> {
                return stat to StatValue(stat, child.forceList(type).map { listChild ->
                    try {
                        stat.createNode(listChild)
                    } catch (ex: SerializationException) {
                        throw SerializationException(listChild, type, "Could not deserialize stat value for '${stat.key}'", ex)
                    }
                }.toMutableList())
            }

            val stats = StatMap(node.childrenMap().map { (_, child) ->
                val statKey = parseNodeNamespacedKey(type, child)
                val stat = stat(statKey)
                    ?: throw SerializationException(child, type, "Invalid stat type '$statKey'")
                createStat(stat, child)
            }.associate { it }.toMutableMap())
            return Profile(stats)
        }
    }
}

data class StatsInstance(
    val stats: StatMap
) : SokolComponent {
    override val componentType get() = StatsInstance::class
}

fun ComponentMapper<StatsInstance>.statMap(entity: SokolEntity): StatMap {
    if (!has(entity)) {
        entity.call(StatsSystem.BuildStats)
    }
    return get(entity).stats
}

object StatsInstanceTarget : SokolSystem

@All(Stats::class)
@None(StatsInstance::class)
@Before(StatsInstanceTarget::class)
class StatsSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mStats = ids.mapper<Stats>()
    private val mStatsInstance = ids.mapper<StatsInstance>()

    object BuildStats : SokolEvent

    @Subscribe
    fun on(event: BuildStats, entity: SokolEntity) {
        val stats = mStats.get(entity).profile

        // todo
        mStatsInstance.set(entity, StatsInstance(stats.stats))
    }
}
