package com.gitlab.aecsocket.sokol.core

import net.kyori.adventure.key.Key

private const val PROFILE = "profile"

interface SokolAPI {
    val engine: SokolEngine
    val persistence: SokolPersistence

    fun entityProfile(id: String): EntityProfile?

    fun componentType(key: Key): ComponentType?
}

class PersistenceException(message: String? = null, cause: Throwable? = null)
    : RuntimeException(message, cause)

abstract class SokolPersistence(private val sokol: SokolAPI) {
    abstract fun newTag(): CompoundNBTTag.Mutable

    fun readBlueprint(tag: NBTTag): EntityBlueprint {
        val compound = tag.asCompound()

        val profileId = try {
            compound.get(PROFILE) { asString() }
        } catch (ex: IllegalStateException) {
            throw PersistenceException(cause = ex)
        }

        val entityProfile = sokol.entityProfile(profileId)
            ?: throw PersistenceException("Invalid entity profile '$profileId'")

        val components = entityProfile.componentProfiles.map { (key, profile) ->
            val config = compound[key.asString()] ?: tag.makeCompound()
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

    private fun writeComponentMap(map: ComponentMap, tag: CompoundNBTTag.Mutable) {
        map.all().forEach { component ->
            if (component is PersistentComponent) {
                tag.set(component.key.asString(), component::write)
            }
        }
    }

    fun writeBlueprint(blueprint: EntityBlueprint, tag: CompoundNBTTag.Mutable = newTag()): CompoundNBTTag.Mutable {
        writeComponentMap(blueprint.components, tag)
        tag.set(PROFILE) { makeString(blueprint.profile.id) }

        return tag
    }

    fun writeEntity(entity: SokolEntity, tag: CompoundNBTTag.Mutable = newTag()): CompoundNBTTag.Mutable {
        writeComponentMap(entity.components, tag)
        tag.set(PROFILE) { makeString(entity.profile.id) }

        return tag
    }
}
