package com.gitlab.aecsocket.sokol.core.serializer

import com.gitlab.aecsocket.alexandria.core.extension.force
import com.gitlab.aecsocket.alexandria.core.extension.forceMap
import com.gitlab.aecsocket.sokol.core.util.RenderMesh
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

private const val TYPE = "type"
private const val STATIC = "static"
private const val DYNAMIC = "dynamic"

object RenderMeshSerializer : TypeSerializer<RenderMesh> {
    override fun serialize(type: Type, obj: RenderMesh?, node: ConfigurationNode) {}

    override fun deserialize(type: Type, node: ConfigurationNode): RenderMesh {
        node.forceMap(type)
        return when (val type = node.node(TYPE).force<String>()) {
            STATIC -> node.force<RenderMesh.Static>()
            DYNAMIC -> node.force<RenderMesh.Dynamic>()
            else -> throw SerializationException("Invalid mesh type '$type'")
        }
    }
}
