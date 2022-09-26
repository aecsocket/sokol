package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.glossa.core.force
import com.gitlab.aecsocket.sokol.paper.util.validateNamespacedKey
import com.gitlab.aecsocket.sokol.paper.util.validateStringKey
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
        val key = validateNamespacedKey(type, node)
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
        val key = validateNamespacedKey(type, node)
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
            validateStringKey(type, node),
            node.force(),
        )
    }
}

class EntityBlueprintSerializer(
    private val sokol: Sokol
) : TypeSerializer<KeyedEntityBlueprint> {
    override fun serialize(type: Type, obj: KeyedEntityBlueprint?, node: ConfigurationNode) {}

    override fun deserialize(type: Type, node: ConfigurationNode): KeyedEntityBlueprint {
        return KeyedEntityBlueprint(sokol,
            validateStringKey(type, node),
            node.force(),
        )
    }
}
