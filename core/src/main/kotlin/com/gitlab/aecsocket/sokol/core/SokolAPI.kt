package com.gitlab.aecsocket.sokol.core

import com.gitlab.aecsocket.alexandria.core.extension.force
import com.gitlab.aecsocket.alexandria.core.keyed.Keyed
import net.kyori.adventure.key.Key
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.serialize.SerializationException

private const val PROFILE = "profile"
private const val FLAGS = "flags"

interface SokolAPI {
    val engine: SokolEngine
    val persistence: SokolPersistence

    fun entityProfile(id: String): KeyedEntityProfile?

    fun componentType(key: Key): ComponentType?
}

class PersistenceException(message: String? = null, cause: Throwable? = null)
    : RuntimeException(message, cause)

abstract class SokolPersistence(private val sokol: SokolAPI) {
    private lateinit var mProfiled: ComponentMapper<Profiled>

    fun enable() {
        mProfiled = sokol.engine.mapper()
    }

    abstract fun tagContext(): NBTTagContext

    private fun componentsOf(profile: EntityProfile) = profile.components.map { (_, profile) ->
        sokol.engine.mapper(profile.componentType) to ComponentFactory { profile.createEmpty() }
    }

    fun emptyBlueprint(profile: EntityProfile, flags: Int = 0): EntityBlueprint {
        return EntityBlueprint(flags, componentsOf(profile))
    }

    fun emptyKeyedBlueprint(profile: KeyedEntityProfile, flags: Int = 0): KeyedEntityBlueprint {
        return KeyedEntityBlueprint(profile.id, flags, componentsOf(profile))
            .with(mProfiled) { Profiled(profile.id) }
    }

    fun readProfileBlueprint(tag: CompoundNBTTag, profile: EntityProfile): EntityBlueprint {
        val flags = tag.getOr(FLAGS) { asInt() } ?: 0
        val components: List<Pair<ComponentMapper<*>, ComponentFactory<*>>> = profile.components.map { (key, profile) ->
            val mapper = sokol.engine.mapper(profile.componentType)
            mapper to try {
                tag[key.asString()]?.let { config -> ComponentFactory { profile.read(it, config) } }
                    ?: ComponentFactory { profile.createEmpty() }
            } catch (ex: SerializationException) {
                throw PersistenceException("Could not read component $key", ex)
            }
        }

        return if (profile is Keyed) KeyedEntityBlueprint(profile.id, flags, components)
            .with(mProfiled) { Profiled(profile.id) }
        else EntityBlueprint(flags, components)
    }

    fun readBlueprint(tag: CompoundNBTTag): EntityBlueprint {
        val profileId = tag.get(PROFILE) { asString() }
        val entityProfile = sokol.entityProfile(profileId)
            ?: throw PersistenceException("Invalid entity profile '$profileId'")

        return readProfileBlueprint(tag, entityProfile)
    }

    fun deserializeProfileBlueprint(node: ConfigurationNode, profile: EntityProfile): EntityBlueprint {
        val flags = node.node(FLAGS).get { 0 }
        val components: List<Pair<ComponentMapper<*>, ComponentFactory<*>>> = profile.components.map { (key, profile) ->
            val mapper = sokol.engine.mapper(profile.componentType)
            val config = node.node(key.asString())

            mapper to try {
                if (config.empty()) ComponentFactory { profile.createEmpty() }
                else ComponentFactory { profile.deserialize(it, config) }
            } catch (ex: SerializationException) {
                throw SerializationException(config, SokolEntity::class.java, "Could not read component $key", ex)
            }
        }

        return if (profile is Keyed) KeyedEntityBlueprint(profile.id, flags, components)
            .with(mProfiled) { Profiled(profile.id) }
        else EntityBlueprint(flags, components)
    }

    fun deserializeBlueprint(node: ConfigurationNode): EntityBlueprint {
        val profileId = node.node(PROFILE).force<String>()
        val entityProfile = sokol.entityProfile(profileId)
            ?: throw PersistenceException("Invalid entity profile '$profileId'")

        return deserializeProfileBlueprint(node, entityProfile)
    }

    fun writeEntity(entity: SokolEntity, tag: CompoundNBTTag = tagContext().makeCompound()): CompoundNBTTag {
        if (entity.flags != 0) tag.set(FLAGS) { makeInt(entity.flags) }
        entity.allComponents().forEach { component ->
            when (component) {
                is Profiled -> tag.set(PROFILE) { makeString(component.id) }
                is PersistentComponent -> {
                    try {
                        tag.set(component.key.asString(), component::write)
                    } catch (ex: PersistenceException) {
                        throw PersistenceException("Could not write component ${component.key}", ex)
                    }
                }
            }
        }
        return tag
    }

    fun writeEntityDelta(entity: SokolEntity, tag: CompoundNBTTag = tagContext().makeCompound()): CompoundNBTTag {
        if (entity.flags != 0) tag.set(FLAGS) { makeInt(entity.flags) }
        entity.allComponents().forEach { component ->
            if (component is PersistentComponent && component.dirty) {
                try {
                    val sKey = component.key.asString()
                    tag[sKey] = tag[sKey]?.let { component.writeDelta(it) } ?: component.write(tag)
                } catch (ex: PersistenceException) {
                    throw PersistenceException("Could not write component delta ${component.key}", ex)
                }
            }
        }
        return tag
    }

    fun serializeEntity(entity: SokolEntity, node: ConfigurationNode) {
        node.node(FLAGS).set(entity.flags)
        entity.allComponents().forEach { component ->
            when (component) {
                is Profiled -> node.node(PROFILE).set(component.id)
                is PersistentComponent -> {
                    try {
                        component.serialize(node.node(component.key.asString()))
                    } catch (ex: SerializationException) {
                        throw SerializationException(node, SokolEntity::class.java, "Could not write component ${component.key}", ex)
                    }
                }
            }
        }
    }
}
