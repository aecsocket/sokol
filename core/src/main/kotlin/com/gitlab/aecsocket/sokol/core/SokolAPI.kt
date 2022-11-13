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

    private fun setProfile(entity: SokolEntity, profile: EntityProfile) {
        if (profile is Keyed) {
            mProfiled.set(entity, Profiled(profile.id))
        }
    }

    fun readEmptyProfile(profile: EntityProfile, space: SokolSpaceAccess, flags: Int = 0): SokolEntity {
        val components = profile.components.map { (_, profile) -> profile.createEmpty() }
        val entity = space.createEntity(flags, components)
        setProfile(entity, profile)
        return entity
    }

    fun readProfileEntities(tag: CompoundNBTTag, profile: EntityProfile, space: SokolSpaceAccess) {
        val flags = tag.getOr(FLAGS) { asInt() } ?: 0
        val components = profile.components.map { (key, profile) ->
            try {
                tag[key.asString()]?.let { profile.read(space, it) } ?: profile.createEmpty()
            } catch (ex: PersistenceException) {
                throw PersistenceException("Could not read component $key", ex)
            }
        }

        val entity = space.createEntity(flags, components)
        setProfile(entity, profile)
    }

    fun readEntities(tag: CompoundNBTTag, space: SokolSpaceAccess) {
        val profileId = tag.get(PROFILE) { asString() }
        val entityProfile = sokol.entityProfile(profileId)
            ?: throw PersistenceException("Invalid entity profile '$profileId'")

        readProfileEntities(tag, entityProfile, space)
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

    fun deserializeProfileEntities(node: ConfigurationNode, profile: EntityProfile, space: SokolSpace) {
        val flags = node.node(FLAGS).get { 0 }
        val components = profile.components.map { (key, profile) ->
            val config = node.node(key.asString())
            try {
                if (config.empty()) profile.createEmpty() else profile.deserialize(space, config)
            } catch (ex: SerializationException) {
                throw SerializationException(config, SokolEntity::class.java, "Could not read component $key", ex)
            }
        }

        space.createEntity(flags, components)
    }

    fun deserializeEntities(node: ConfigurationNode, space: SokolSpace) {
        val profileId = node.node(PROFILE).force<String>()
        val entityProfile = sokol.entityProfile(profileId)
            ?: throw PersistenceException("Invalid entity profile '$profileId'")

        deserializeProfileEntities(node, entityProfile, space)
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
