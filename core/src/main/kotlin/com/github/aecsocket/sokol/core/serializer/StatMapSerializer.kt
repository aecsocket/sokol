package com.github.aecsocket.sokol.core.serializer

import com.github.aecsocket.alexandria.core.extension.forceList
import com.github.aecsocket.alexandria.core.extension.forceMap
import com.github.aecsocket.sokol.core.stat.*
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

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
