package com.gitlab.aecsocket.sokol.core

import com.gitlab.aecsocket.alexandria.core.extension.force
import com.gitlab.aecsocket.alexandria.core.keyed.parseNodeAlexandriaKey
import com.gitlab.aecsocket.alexandria.core.keyed.parseNodeNamespacedKey
import net.kyori.adventure.key.Key
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

private const val PROFILE = "profile"

class ComponentProfileSerializer(private val sokol: SokolAPI) : TypeSerializer<ComponentProfile> {
    override fun serialize(type: Type, obj: ComponentProfile?, node: ConfigurationNode) {}

    override fun deserialize(type: Type, node: ConfigurationNode): ComponentProfile {
        val componentTypeKey = parseNodeNamespacedKey(type, node)
        val componentType = sokol.componentType(componentTypeKey)
            ?: throw SerializationException(node, type, "Invalid component type '$componentTypeKey'")
        return try {
            componentType.createProfile(node)
        } catch (ex: ComponentPersistenceException) {
            throw SerializationException(node, type, "Could not create profile from type", ex)
        }
    }
}

class EntityProfilerSerializer(private val sokol: SokolAPI) : TypeSerializer<EntityProfile> {
    override fun serialize(type: Type, obj: EntityProfile?, node: ConfigurationNode) {}

    override fun deserialize(type: Type, node: ConfigurationNode): EntityProfile {
        val id = parseNodeAlexandriaKey(type, node)
        val componentProfiles = node.force<HashMap<Key, ComponentProfile>>()
        return EntityProfile(id, componentProfiles)
    }
}

private fun writeComponentMap(map: ComponentMap, node: ConfigurationNode) {
    map.all().forEach { component ->
        if (component is PersistentComponent) {
            component.write(node.node(component.key.asString()))
        }
    }
}

class EntityBlueprintSerializer(private val sokol: SokolAPI) : TypeSerializer<EntityBlueprint> {
    override fun serialize(type: Type, obj: EntityBlueprint?, node: ConfigurationNode) {
        if (obj == null) node.set(null)
        else {
            writeComponentMap(obj.components, node)
            node.node(PROFILE).set(obj.profile.id)
        }
    }

    override fun deserialize(type: Type, node: ConfigurationNode): EntityBlueprint {
        val profileId = (if (node.isMap) node.node(PROFILE) else node).force<String>()
        val entityProfile = sokol.entityProfile(profileId)
            ?: throw SerializationException(node, type, "Invalid entity profile '$profileId'")

        val components = entityProfile.componentProfiles.map { (key, profile) ->
            val config = node.node(key.asString())
            try {
                profile.read(config)
            } catch (ex: PersistenceException) {
                throw SerializationException(config, type, "Could not read component $key")
            }
        }

        return EntityBlueprint(
            entityProfile,
            sokol.engine.componentMap(components)
        )
    }
}

object SokolEntitySerializer : TypeSerializer<SokolEntity> {
    override fun serialize(type: Type, obj: SokolEntity?, node: ConfigurationNode) {
        if (obj == null) node.set(null)
        else {
            writeComponentMap(obj.components, node)
            node.node(PROFILE).set(obj.profile.id)
        }
    }

    override fun deserialize(type: Type, node: ConfigurationNode): SokolEntity {
        throw UnsupportedOperationException()
    }
}
