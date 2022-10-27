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
    abstract fun newTag(): CompoundNBTTag.Mutable

    fun readBlueprintByProfile(entityProfile: EntityProfile, tag: CompoundNBTTag): EntityBlueprint {
        val components = entityProfile.componentProfiles.map { (key, profile) ->
            val config = tag[key.asString()] ?: tag.makeCompound()
            try {
                profile.read(config)
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
        val profileId = try {
            tag.get(PROFILE) { asString() }
        } catch (ex: IllegalStateException) {
            throw PersistenceException(cause = ex)
        }

        val entityProfile = sokol.entityProfile(profileId)
            ?: throw PersistenceException("Invalid entity profile '$profileId'")

        return readBlueprintByProfile(entityProfile, tag)
    }

    private fun writeComponentMap(map: ComponentMap, tag: CompoundNBTTag.Mutable) {
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

    private fun writeProfile(profile: EntityProfile, tag: CompoundNBTTag.Mutable) {
        if (profile is Keyed) {
            tag.set(PROFILE) { makeString(profile.id) }
        }
    }

    fun writeBlueprint(blueprint: EntityBlueprint, tag: CompoundNBTTag.Mutable = newTag()): CompoundNBTTag.Mutable {
        writeProfile(blueprint.profile, tag)
        writeComponentMap(blueprint.components, tag)
        return tag
    }

    fun writeEntity(entity: SokolEntity, tag: CompoundNBTTag.Mutable = newTag()): CompoundNBTTag.Mutable {
        writeProfile(entity.profile, tag)
        writeComponentMap(entity.components, tag)
        return tag
    }
}
