package com.github.aecsocket.sokol.core.serializer

import com.github.aecsocket.alexandria.core.keyed.Keyed
import com.github.aecsocket.sokol.core.*
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

abstract class BlueprintSerializer<
    T : Blueprint<N>,
    N : DataNode
> : TypeSerializer<T> {
    protected abstract val nodeType: Class<N>

    protected abstract fun create(
        id: String,
        node: N
    ): T

    override fun serialize(type: Type, obj: T?, node: ConfigurationNode) =
        throw UnsupportedOperationException()

    override fun deserialize(type: Type, node: ConfigurationNode): T {
        val id = try {
            Keyed.validate(node.key().toString())
        } catch (ex: Keyed.ValidationException) {
            throw SerializationException(node, type, "Invalid key")
        }

        return create(id, node.get(nodeType)
            ?: throw SerializationException(node, type, "Null node"))
    }
}
