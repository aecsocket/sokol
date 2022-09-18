package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.core.keyed.Keyed
import com.gitlab.aecsocket.glossa.core.force
import com.gitlab.aecsocket.sokol.core.SokolComponent
import net.kyori.adventure.key.InvalidKeyException
import net.kyori.adventure.key.Key
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

class ComponentSerializer(
    private val sokol: Sokol
) : TypeSerializer<SokolComponent.Persistent> {
    override fun serialize(type: Type, obj: SokolComponent.Persistent?, node: ConfigurationNode) {
        if (obj == null) node.set(null)
        else {
            obj.serialize(node)
        }
    }

    override fun deserialize(type: Type, node: ConfigurationNode): SokolComponent.Persistent {
        val key = try {
            Key.key(node.key().toString())
        } catch (ex: InvalidKeyException) {
            throw SerializationException(node, type, "Invalid key", ex)
        }
        val componentType = sokol.componentTypes[key.asString()]
            ?: throw SerializationException(node, type, "Invalid component type '$key'")
        return componentType.deserialize(node)
    }
}

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
}

class EntityBlueprintSerializer(
    private val sokol: Sokol
) : TypeSerializer<EntityBlueprint> {
    override fun serialize(type: Type, obj: EntityBlueprint?, node: ConfigurationNode) {}

    override fun deserialize(type: Type, node: ConfigurationNode): EntityBlueprint {
        return EntityBlueprint(sokol,
            try {
                Keyed.validate(node.key().toString())
            } catch (ex: Keyed.ValidationException) {
                throw SerializationException(node, type, "Invalid key", ex)
            },
            node.childrenMap().map { (_, child) -> child.force<SokolComponent.Persistent>() },
        )
    }
}
