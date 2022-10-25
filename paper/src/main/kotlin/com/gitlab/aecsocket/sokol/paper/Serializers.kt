package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.core.keyed.parseNodeAlexandriaKey
import com.gitlab.aecsocket.alexandria.core.keyed.parseNodeNamespacedKey
import com.gitlab.aecsocket.glossa.core.force
import com.gitlab.aecsocket.sokol.core.SokolBlueprint
import com.gitlab.aecsocket.sokol.core.SokolComponent
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

internal fun componentTypeFrom(sokol: Sokol, type: Type, node: ConfigurationNode): PersistentComponentType {
    val key = parseNodeNamespacedKey(type, node)
    return sokol.componentType(key)
        ?: throw SerializationException(node, type, "Invalid component type '$key', valid: ${sokol.componentTypes.keys}")
}

class ComponentSerializer(
    private val sokol: Sokol
) : TypeSerializer<SokolComponent> {
    override fun serialize(type: Type, obj: SokolComponent?, node: ConfigurationNode) {
        if (obj == null) node.set(null)
        else if (obj is PersistentComponent) {
            obj.write(node)
        }
    }

    override fun deserialize(type: Type, node: ConfigurationNode): SokolComponent {
        val componentType = componentTypeFrom(sokol, type, node)
        return try {
            componentType.read(node)
        } catch (ex: ComponentSerializationException) {
            throw SerializationException(node, type, ex)
        }
    }
}

class ComponentFactorySerializer(
    private val sokol: Sokol
) : TypeSerializer<PersistentComponentFactory> {
    override fun serialize(type: Type, obj: PersistentComponentFactory?, node: ConfigurationNode) {}

    override fun deserialize(type: Type, node: ConfigurationNode): PersistentComponentFactory {
        val componentType = componentTypeFrom(sokol, type, node)
        return try {
            componentType.readFactory(node)
        } catch (ex: ComponentSerializationException) {
            throw SerializationException(node, type, ex)
        }
    }
}

class BlueprintSerializer(
    private val sokol: Sokol
) : TypeSerializer<SokolBlueprint> {
    override fun serialize(type: Type, obj: SokolBlueprint?, node: ConfigurationNode) {
        if (obj == null) node.set(null)
        else {
            obj.components.forEach { component ->
                if (component is PersistentComponent) {
                    node.node(component.key.toString()).set(component)
                }
            }
        }
    }

    override fun deserialize(type: Type, node: ConfigurationNode) = SokolBlueprint(
        node.force<HashMap<String, SokolComponent>>().map { it.value }
    )
}

class ItemBlueprintSerializer(
    private val sokol: Sokol
) : TypeSerializer<KeyedItemBlueprint> {
    override fun serialize(type: Type, obj: KeyedItemBlueprint?, node: ConfigurationNode) {}

    override fun deserialize(type: Type, node: ConfigurationNode): KeyedItemBlueprint {
        return KeyedItemBlueprint(sokol,
            parseNodeAlexandriaKey(type, node),
            node.force<HashMap<String, PersistentComponentFactory>>(),
        )
    }
}

class EntityBlueprintSerializer(
    private val sokol: Sokol
) : TypeSerializer<KeyedMobBlueprint> {
    override fun serialize(type: Type, obj: KeyedMobBlueprint?, node: ConfigurationNode) {}

    override fun deserialize(type: Type, node: ConfigurationNode): KeyedMobBlueprint {
        return KeyedMobBlueprint(sokol,
            parseNodeAlexandriaKey(type, node),
            node.force<HashMap<String, PersistentComponentFactory>>(),
        )
    }
}
