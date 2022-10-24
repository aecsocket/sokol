package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.core.keyed.parseNodeAlexandriaKey
import com.gitlab.aecsocket.alexandria.core.keyed.parseNodeNamespacedKey
import com.gitlab.aecsocket.glossa.core.force
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

class ComponentSerializer(
    private val sokol: Sokol
) : TypeSerializer<PersistentComponent> {
    override fun serialize(type: Type, obj: PersistentComponent?, node: ConfigurationNode) {
        if (obj == null) node.set(null)
        else {
            obj.write(node)
        }
    }

    override fun deserialize(type: Type, node: ConfigurationNode): PersistentComponent {
        val key = parseNodeNamespacedKey(type, node)
        val componentType = sokol.componentType(key)
            ?: throw SerializationException(node, type, "Invalid component type '$key', valid: ${sokol.componentTypes.keys}")
        return componentType.read(node)
    }
}

class ComponentFactorySerializer(
    private val sokol: Sokol
) : TypeSerializer<PersistentComponentFactory> {
    override fun serialize(type: Type, obj: PersistentComponentFactory?, node: ConfigurationNode) {}

    override fun deserialize(type: Type, node: ConfigurationNode): PersistentComponentFactory {
        val key = parseNodeNamespacedKey(type, node)
        val componentType = sokol.componentType(key)
            ?: throw SerializationException(node, type, "Invalid component type '$key'")
        return componentType.readFactory(node)
    }
}

class ItemBlueprintSerializer(
    private val sokol: Sokol
) : TypeSerializer<KeyedItemBlueprint> {
    override fun serialize(type: Type, obj: KeyedItemBlueprint?, node: ConfigurationNode) {}

    override fun deserialize(type: Type, node: ConfigurationNode): KeyedItemBlueprint {
        return KeyedItemBlueprint(sokol,
            parseNodeAlexandriaKey(type, node),
            node.force<MutableMap<String, PersistentComponentFactory>>(),
        )
    }
}

class EntityBlueprintSerializer(
    private val sokol: Sokol
) : TypeSerializer<KeyedEntityBlueprint> {
    override fun serialize(type: Type, obj: KeyedEntityBlueprint?, node: ConfigurationNode) {}

    override fun deserialize(type: Type, node: ConfigurationNode): KeyedEntityBlueprint {
        return KeyedEntityBlueprint(sokol,
            parseNodeAlexandriaKey(type, node),
            node.force<MutableMap<String, PersistentComponentFactory>>(),
        )
    }
}
