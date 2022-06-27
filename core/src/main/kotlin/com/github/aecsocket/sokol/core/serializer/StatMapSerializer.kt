package com.github.aecsocket.sokol.core.serializer

import com.github.aecsocket.sokol.core.stat.Stat
import com.github.aecsocket.sokol.core.stat.StatMap
import net.kyori.adventure.key.Key
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

class StatMapSerializer(
    var types: Map<String, Stat<*>>
) : TypeSerializer<StatMap> {
    override fun serialize(type: Type, obj: StatMap?, node: ConfigurationNode) =
        throw UnsupportedOperationException()

    override fun deserialize(type: Type, node: ConfigurationNode): StatMap {
        TODO("...")
    }
}