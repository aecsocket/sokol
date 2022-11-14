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

    private fun SokolEntity.set(component: SokolComponent) =
        setComponent(sokol.engine.idOf(component.componentType), component)

    private fun setProfile(entity: SokolEntity, profile: EntityProfile) {
        if (profile is KeyedEntityProfile)
            mProfiled.set(entity, Profiled(profile))
    }

    fun emptyEntity(profile: EntityProfile): SokolEntity {
        val entity = sokol.engine.newEntity()

        profile.components.forEach { (_, profile) ->
            val component = profile.createEmpty(entity)
            entity.set(component)
        }

        setProfile(entity, profile)
        return entity
    }

    fun readProfiledEntity(tag: CompoundNBTTag, profile: EntityProfile): SokolEntity {
        val entity = sokol.engine.newEntity()

        entity.flags = tag.getOr(FLAGS) { asInt() } ?: 0

        profile.components.forEach { (key, profile) ->
            val component = try {
                tag[key.asString()]?.let { config ->
                    profile.read(config, entity)
                } ?: profile.createEmpty(entity)
            } catch (ex: PersistenceException) {
                throw PersistenceException("Could not read component $key", ex)
            }

            entity.set(component)
        }

        setProfile(entity, profile)

        return entity
    }

    fun readEntity(tag: CompoundNBTTag): SokolEntity {
        val profileId = tag.get(PROFILE) { asString() }
        val entityProfile = sokol.entityProfile(profileId)
            ?: throw PersistenceException("Invalid entity profile '$profileId'")

        return readProfiledEntity(tag, entityProfile)
    }

    fun deserializeProfiledEntity(node: ConfigurationNode, profile: EntityProfile): SokolEntity {
        val entity = sokol.engine.newEntity()

        entity.flags = node.node(FLAGS).get { 0 }

        profile.components.forEach { (key, profile) ->
            val config = node.node(key.asString())
            val component = try {
                if (config.empty()) profile.createEmpty(entity) else profile.deserialize(config, entity)
            } catch (ex: SerializationException) {
                throw SerializationException(config, SokolEntity::class.java, "Could not read component $key", ex)
            }

            entity.set(component)
        }

        setProfile(entity, profile)

        return entity
    }

    fun deserializeEntity(node: ConfigurationNode): SokolEntity {
        val profileId = node.node(PROFILE).force<String>()
        val entityProfile = sokol.entityProfile(profileId)
            ?: throw PersistenceException("Invalid entity profile '$profileId'")

        return deserializeProfiledEntity(node, entityProfile)
    }

    fun writeEntity(entity: SokolEntity, tag: CompoundNBTTag = tagContext().makeCompound()): CompoundNBTTag {
        tag.set(FLAGS) { makeInt(entity.flags) }

        entity.components.forEach { component ->
            when (component) {
                is Profiled -> tag.set(PROFILE) { makeString(component.profile.id) }
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
        tag.set(FLAGS) { makeInt(entity.flags) }

        entity.components.forEach { component ->
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

        entity.components.forEach { component ->
            when (component) {
                is Profiled -> node.node(PROFILE).set(component.profile.id)
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
