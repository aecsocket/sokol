package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.core.keyed.Keyed
import com.gitlab.aecsocket.glossa.core.KeyValidationException
import com.gitlab.aecsocket.glossa.core.force
import com.gitlab.aecsocket.sokol.core.ItemDescriptor
import com.gitlab.aecsocket.sokol.core.SokolComponent
import net.kyori.adventure.key.InvalidKeyException
import net.kyori.adventure.key.Key
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

private const val ITEM = "item"
private const val COMPONENTS = "components"

class ComponentSerializer(
    private val sokol: Sokol
) : TypeSerializer<SokolComponent> {
    override fun serialize(type: Type, obj: SokolComponent?, node: ConfigurationNode) {
        if (obj == null) node.set(null)
        else {
            obj.serialize(node)
        }
    }

    override fun deserialize(type: Type, node: ConfigurationNode): SokolComponent {
        val key = try {
            Key.key(node.key().toString())
        } catch (ex: InvalidKeyException) {
            throw SerializationException(node, type, "Invalid key", ex)
        }
        val componentType = sokol.componentTypes[key]
            ?: throw SerializationException(node, type, "Invalid component type '$key'")
        return componentType.deserialize(node)
    }
}

class ItemBlueprintSerializer(
    private val sokol: Sokol
) : TypeSerializer<ItemBlueprint> {
    override fun serialize(type: Type, obj: ItemBlueprint?, node: ConfigurationNode) {}

    override fun deserialize(type: Type, node: ConfigurationNode): ItemBlueprint {
        val key = try {
            Keyed.validate(node.key().toString())
        } catch (ex: KeyValidationException) {
            throw SerializationException(node, type, "Invalid key", ex)
        }

        val components = node.node(COMPONENTS).childrenMap()
            .map { (_, child) -> child.force<SokolComponent>() }

        return ItemBlueprint(sokol,
            key,
            node.node(ITEM).force(),
            components,
        )
    }
}
