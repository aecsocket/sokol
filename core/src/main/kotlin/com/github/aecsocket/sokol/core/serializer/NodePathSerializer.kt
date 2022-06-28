package com.github.aecsocket.sokol.core.serializer

import com.github.aecsocket.alexandria.core.extension.force
import com.github.aecsocket.alexandria.core.extension.forceList
import com.github.aecsocket.sokol.core.*
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

object NodePathSerializer : TypeSerializer<NodePath> {
    override fun serialize(type: Type, obj: NodePath?, node: ConfigurationNode) {
        if (obj == null) node.set(null)
        else {
            node.setList(String::class.java, obj.toList())
        }
    }

    override fun deserialize(type: Type, node: ConfigurationNode): NodePath {
        return NodePath.of(node.forceList(type).map { it.force() })
    }
}
