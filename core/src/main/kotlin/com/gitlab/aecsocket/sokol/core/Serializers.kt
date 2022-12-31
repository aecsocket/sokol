package com.gitlab.aecsocket.sokol.core

import com.gitlab.aecsocket.alexandria.core.extension.force
import com.gitlab.aecsocket.alexandria.core.keyed.parseNodeAlexandriaKey
import com.gitlab.aecsocket.alexandria.core.keyed.parseNodeNamespacedKey
import net.kyori.adventure.key.Key
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

class ComponentProfileSerializer(private val sokol: SokolAPI) : TypeSerializer<ComponentProfile<*>> {
    override fun serialize(type: Type, obj: ComponentProfile<*>?, node: ConfigurationNode) {}

    override fun deserialize(type: Type, node: ConfigurationNode): ComponentProfile<*> {
        val componentTypeKey = parseNodeNamespacedKey(type, node)
        val componentType = sokol.componentType(componentTypeKey)
            ?: throw SerializationException(node, type, "Invalid component type '$componentTypeKey'")
        return componentType.createProfile(node)
    }
}

object EntityProfileSerializer : TypeSerializer<EntityProfile> {
    override fun serialize(type: Type, obj: EntityProfile?, node: ConfigurationNode) {}

    override fun deserialize(type: Type, node: ConfigurationNode): EntityProfile {
        val componentProfiles = node.force<HashMap<Key, ComponentProfile<*>>>()
        return EntityProfile(componentProfiles)
    }
}

object KeyedEntityProfileSerializer : TypeSerializer<KeyedEntityProfile> {
    override fun serialize(type: Type, obj: KeyedEntityProfile?, node: ConfigurationNode) {}

    override fun deserialize(type: Type, node: ConfigurationNode): KeyedEntityProfile {
        val id = parseNodeAlexandriaKey(type, node)
        val componentProfiles = node.force<HashMap<Key, ComponentProfile<*>>>()
        return KeyedEntityProfile(id, componentProfiles)
    }
}

class EntitySerializer(private val sokol: SokolAPI) : TypeSerializer<SokolEntity> {
    override fun serialize(type: Type, obj: SokolEntity?, node: ConfigurationNode) {
        if (obj == null) node.set(null)
        else {
            sokol.persistence.serializeEntity(obj, node)
        }
    }

    override fun deserialize(type: Type, node: ConfigurationNode) = throw UnsupportedOperationException()
}

class BlueprintSerializer(private val sokol: SokolAPI) : TypeSerializer<EntityBlueprint> {
    override fun serialize(type: Type, obj: EntityBlueprint?, node: ConfigurationNode) {}

    override fun deserialize(type: Type, node: ConfigurationNode) = sokol.persistence.deserializeBlueprint(node)
}
