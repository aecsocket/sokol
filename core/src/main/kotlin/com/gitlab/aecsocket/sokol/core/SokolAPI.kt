package com.gitlab.aecsocket.sokol.core

import com.gitlab.aecsocket.alexandria.core.keyed.Keyed
import net.kyori.adventure.key.Key

private const val PROFILE = "profile"

interface SokolAPI {
    val engine: SokolEngine
    val persistence: SokolPersistence

    fun entityProfile(id: String): KeyedEntityProfile?

    fun componentType(key: Key): ComponentType?
}

class PersistenceException(message: String? = null, cause: Throwable? = null)
    : RuntimeException(message, cause)

abstract class SokolPersistence(private val sokol: SokolAPI) {
    abstract fun tagContext(): NBTTagContext

    fun readBlueprintByProfile(tag: CompoundNBTTag, entityProfile: EntityProfile): EntityBlueprint {
        val components = entityProfile.componentProfiles.map { (key, profile) ->
            try {
                tag[key.asString()]?.let { profile.read(it) } ?: profile.readEmpty()
            } catch (ex: PersistenceException) {
                throw PersistenceException("Could not read component $key", ex)
            }
        }

        return EntityBlueprint(
            entityProfile,
            sokol.engine.componentMap(components)
        )
    }

    fun readBlueprint(tag: CompoundNBTTag): EntityBlueprint {
        val profileId = tag.get(PROFILE) { asString() }
        val entityProfile = sokol.entityProfile(profileId)
            ?: throw PersistenceException("Invalid entity profile '$profileId'")

        return readBlueprintByProfile(tag, entityProfile)
    }

    private fun writeComponentDeltas(map: ComponentMap, tag: CompoundNBTTag) {
        map.all().forEach { component ->
            if (component is PersistentComponent && component.dirty) {
                try {
                    val sKey = component.key.asString()
                    tag[sKey] = tag[sKey]?.let { component.writeDelta(it) } ?: component.write(tag)
                } catch (ex: PersistenceException) {
                    throw PersistenceException("Could not write component ${component.key}", ex)
                }
            }
        }
    }

    private fun writeComponents(map: ComponentMap, tag: CompoundNBTTag) {
        map.all().forEach { component ->
            if (component is PersistentComponent) {
                try {
                    tag.set(component.key.asString(), component::write)
                } catch (ex: PersistenceException) {
                    throw PersistenceException("Could not write component ${component.key}", ex)
                }
            }
        }
    }

    fun writeEntityDeltas(entity: SokolEntity, tag: CompoundNBTTag = tagContext().makeCompound()): CompoundNBTTag {
        writeComponentDeltas(entity.components, tag)
        return tag
    }

    private fun writeProfile(profile: EntityProfile, tag: CompoundNBTTag) {
        if (profile is Keyed) {
            tag.set(PROFILE) { makeString(profile.id) }
        }
    }

    fun writeBlueprint(blueprint: EntityBlueprint, tag: CompoundNBTTag = tagContext().makeCompound()): CompoundNBTTag {
        writeProfile(blueprint.profile, tag)
        writeComponents(blueprint.components, tag)
        return tag
    }

    fun writeEntity(entity: SokolEntity, tag: CompoundNBTTag = tagContext().makeCompound()): CompoundNBTTag {
        writeProfile(entity.profile, tag)
        writeComponents(entity.components, tag)
        return tag
    }
}
