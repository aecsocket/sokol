package com.gitlab.aecsocket.sokol.core

import com.gitlab.aecsocket.alexandria.core.extension.force
import com.gitlab.aecsocket.alexandria.core.keyed.Keyed
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

object EntityProfileSerializer : TypeSerializer<EntityProfile> {
    override fun serialize(type: Type, obj: EntityProfile?, node: ConfigurationNode) {}

    override fun deserialize(type: Type, node: ConfigurationNode): EntityProfile {
        val componentProfiles = node.force<HashMap<Key, ComponentProfile>>()
        return SimpleEntityProfile(componentProfiles)
    }
}

object KeyedEntityProfileSerializer : TypeSerializer<KeyedEntityProfile> {
    override fun serialize(type: Type, obj: KeyedEntityProfile?, node: ConfigurationNode) {}

    override fun deserialize(type: Type, node: ConfigurationNode): KeyedEntityProfile {
        val id = parseNodeAlexandriaKey(type, node)
        val componentProfiles = node.force<HashMap<Key, ComponentProfile>>()
        return KeyedEntityProfile(id, componentProfiles)
    }
}

private fun writeComponentMap(map: ComponentMap, node: ConfigurationNode) {
    map.all().forEach { component ->
        if (component is PersistentComponent) {
            val child = node.node(component.key.asString())
            try {
                component.write(child)
            } catch (ex: SerializationException) {
                throw SerializationException(node, ComponentMap::class.java, "Could not write component ${component.key}", ex)
            }
        }
    }
}

private fun writeProfile(profile: EntityProfile, node: ConfigurationNode, type: Type) {
    if (profile is Keyed) {
        node.node(PROFILE).set(profile.id)
    }
}

fun deserializeBlueprintByProfile(
    sokol: SokolAPI,
    entityProfile: EntityProfile,
    node: ConfigurationNode
): EntityBlueprint {
    val components = entityProfile.componentProfiles.map { (key, profile) ->
        val config = node.node(key.asString())
        try {
            if (config.empty()) profile.readEmpty() else profile.read(config)
        } catch (ex: PersistenceException) {
            throw SerializationException(config, EntityBlueprint::class.java, "Could not read component $key", ex)
        }
    }
    val componentMap = sokol.engine.componentMap(components)

    return if (entityProfile is KeyedEntityProfile) KeyedEntityBlueprint(entityProfile, componentMap)
    else EntityBlueprint(entityProfile, componentMap)
}

class EntityBlueprintSerializer(private val sokol: SokolAPI) : TypeSerializer<EntityBlueprint> {
    override fun serialize(type: Type, obj: EntityBlueprint?, node: ConfigurationNode) {
        if (obj == null) node.set(null)
        else {
            writeProfile(obj.profile, node, type)
            writeComponentMap(obj.components, node)
        }
    }

    override fun deserialize(type: Type, node: ConfigurationNode): EntityBlueprint {
        val profileId = (if (node.isMap) node.node(PROFILE) else node).force<String>()
        val entityProfile = sokol.entityProfile(profileId)
            ?: throw SerializationException(node, type, "Invalid entity profile '$profileId'")

        return deserializeBlueprintByProfile(sokol, entityProfile, node)
    }
}

object SokolEntitySerializer : TypeSerializer<SokolEntity> {
    override fun serialize(type: Type, obj: SokolEntity?, node: ConfigurationNode) {
        if (obj == null) node.set(null)
        else {
            writeProfile(obj.profile, node, type)
            writeComponentMap(obj.components, node)
        }
    }

    override fun deserialize(type: Type, node: ConfigurationNode): SokolEntity {
        throw UnsupportedOperationException()
    }
}
