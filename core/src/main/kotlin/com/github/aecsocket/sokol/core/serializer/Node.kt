package com.github.aecsocket.sokol.core.serializer

import com.github.aecsocket.alexandria.core.extension.force
import com.github.aecsocket.alexandria.core.extension.forceList
import com.github.aecsocket.sokol.core.*
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

private const val ID = "id"
private const val FEATURES = "features"
private const val CHILDREN = "children"

object NodePathSerializer : TypeSerializer<NodePath> {
    override fun serialize(type: Type, obj: NodePath?, node: ConfigurationNode) {
        if (obj == null) node.set(null)
        else {
            node.setList(String::class.java, obj.toList())
        }
    }

    override fun deserialize(type: Type, node: ConfigurationNode): NodePath {
        return nodePathOf(node.forceList(type).map { it.force() })
    }
}

abstract class DataNodeSerializer<
    N : DataNode.Scoped<N, C, D, *>,
    C : NodeComponent.Scoped<C, P, *>,
    P : Feature.Profile<D>,
    D : Feature.Data<*>
> : TypeSerializer<N> {
    protected abstract fun component(id: String): C?

    protected abstract fun create(
        value: C,
        features: MutableMap<String, D>,
        parent: NodeKey<N>?,
        children: MutableMap<String, N>
    ): N

    override fun serialize(type: Type, obj: N?, node: ConfigurationNode) {
        fun serialize0(obj: N, node: ConfigurationNode) {
            val features = node.node(FEATURES)
            obj.features.map { (key, feature) ->
                feature.serialize(features.node(key))
            }

            val children = node.node(CHILDREN)
            obj.children.map { (key, child) ->
                serialize0(child, children.node(key))
            }

            (if (features.empty() && children.empty()) node
                else node.node(ID)).set(obj.component.id)
        }

        if (obj == null) node.set(null)
        else {
            serialize0(obj, node)
        }
    }

    override fun deserialize(type: Type, node: ConfigurationNode): N {
        fun deserialize0(node: ConfigurationNode, parent: NodeKey<N>?): N {
            val id = (if (node.isMap) node.node(ID) else node).force<String>()
            val value = component(id)
                ?: throw SerializationException(node, type, "No component with ID '$id'")

            val features = HashMap<String, D>()
            node.node(FEATURES).childrenMap().forEach { (key, data) ->
                val profile = value.features[key]
                    ?: throw SerializationException(data, type, "Component $id does not have feature $key")
                val feature = try {
                    profile.createData(data)
                } catch (ex: SerializationException) {
                    throw SerializationException(data, type, "Could not create feature $key on component $id", ex)
                }
                features[key.toString()] = feature
            }

            val children = HashMap<String, N>()
            val res = create(value, features, parent, children)

            node.node(CHILDREN).childrenMap().forEach { (key, data) ->
                val child = try {
                    deserialize(type, data)
                } catch (ex: SerializationException) {
                    throw SerializationException(data, type, "Could not create child $key", ex)
                }
                children[key.toString()] = child
            }

            return res
        }

        return deserialize0(node, null)
    }
}
