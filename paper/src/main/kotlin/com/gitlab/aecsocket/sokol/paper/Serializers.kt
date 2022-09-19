package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.core.extension.forceMap
import com.gitlab.aecsocket.alexandria.core.keyed.Keyed
import com.gitlab.aecsocket.glossa.core.force
import com.gitlab.aecsocket.sokol.core.SokolBlueprint
import com.gitlab.aecsocket.sokol.core.SokolComponent
import net.kyori.adventure.key.InvalidKeyException
import net.kyori.adventure.key.Key
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
            obj.serialize(node)
        }
    }

    override fun deserialize(type: Type, node: ConfigurationNode): PersistentComponent {
        val key = try { Key.key(node.key().toString())
        } catch (ex: InvalidKeyException) { throw SerializationException(node, type, "Invalid key", ex) }
        val componentType = sokol.componentType(key)
            ?: throw SerializationException(node, type, "Invalid component type '$key', valid: ${sokol.componentTypes.keys}")
        return componentType.deserialize(node)
    }
}

/*
class ItemBlueprintSerializer(
    private val sokol: Sokol
) : TypeSerializer<ItemBlueprint> {
    override fun serialize(type: Type, obj: ItemBlueprint?, node: ConfigurationNode) {}

    override fun deserialize(type: Type, node: ConfigurationNode): ItemBlueprint {
        return ItemBlueprint(sokol,
            try {
                Keyed.validate(node.key().toString())
            } catch (ex: Keyed.ValidationException) {
                throw SerializationException(node, type, "Invalid key", ex)
            },
            node.childrenMap().map { (_, child) -> child.force<SokolComponent.Persistent>() },
        )
    }
}*/

class BlueprintSerializer(
    private val sokol: Sokol
) : TypeSerializer<SokolBlueprint> {
    override fun serialize(type: Type, obj: SokolBlueprint?, node: ConfigurationNode) {
        if (obj == null) node.set(null)
        else {
            obj.components.forEach { component ->
                if (component is PersistentComponent) {
                    node.node(component.key.asString()).set(component)
                }
            }
        }
    }

    override fun deserialize(type: Type, node: ConfigurationNode): SokolBlueprint {
        val components = node.forceMap(type).map { (_, child) -> child.force<PersistentComponent>() }
        return SokolBlueprint(components)
    }
}

class EntityBlueprintSerializer(
    private val sokol: Sokol
) : TypeSerializer<KeyedEntityBlueprint> {
    override fun serialize(type: Type, obj: KeyedEntityBlueprint?, node: ConfigurationNode) {}

    override fun deserialize(type: Type, node: ConfigurationNode): KeyedEntityBlueprint {
        return KeyedEntityBlueprint(sokol,
            try { Keyed.validate(node.key().toString())
            } catch (ex: Keyed.ValidationException) { throw SerializationException(node, type, "Invalid key", ex) },
            node.force(),
        )
    }
}
