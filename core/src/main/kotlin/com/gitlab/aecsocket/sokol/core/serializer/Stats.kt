package com.gitlab.aecsocket.sokol.core.serializer

import com.gitlab.aecsocket.alexandria.core.extension.force
import com.gitlab.aecsocket.alexandria.core.extension.forceList
import com.gitlab.aecsocket.alexandria.core.extension.forceMap
import com.gitlab.aecsocket.sokol.core.rule.Rule
import com.gitlab.aecsocket.sokol.core.stat.*
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

private const val PRIORITY = "priority"
private const val REVERSED = "reversed"
private const val RULE = "rule"

class StatMapSerializer(
    var types: Map<String, Stat<*>> = emptyMap()
) : TypeSerializer<StatMap> {
    override fun serialize(type: Type, obj: StatMap?, node: ConfigurationNode) {}

    override fun deserialize(type: Type, node: ConfigurationNode): StatMap {
        val res = statMapOf()
        node.forceMap(type).forEach { (key, child) ->
            val stat = types[key]
                ?: throw SerializationException(child, type, "No stat type for key '$key' - available: [${types.keys.joinToString()}]")

            fun <T : Any> add(stat: Stat<T>) {
                try {
                    val values = child.forceList(type).map { stat.deserialize(it) }
                    res.set(stat.key, statNodeOf(stat, values))
                } catch (ex: Exception) {
                    throw SerializationException(child, type, "Could not deserialize stat values", ex)
                }
            }

            add(stat)
        }
        return res
    }
}

object ApplicableStatsSerializer : TypeSerializer<ApplicableStats> {
    override fun serialize(type: Type, obj: ApplicableStats?, node: ConfigurationNode) {}

    override fun deserialize(type: Type, node: ConfigurationNode): ApplicableStats {
        val priority = node.node(PRIORITY).get { 0 }
        val reversed = node.node(REVERSED).get { false }
        val rule = node.node(RULE).get<Rule> { Rule.True }
        val stats = node.copy().apply {
            removeChild(PRIORITY)
            removeChild(REVERSED)
            removeChild(RULE)
        }
        return ApplicableStats(stats.force(), priority, reversed, rule)
    }
}
