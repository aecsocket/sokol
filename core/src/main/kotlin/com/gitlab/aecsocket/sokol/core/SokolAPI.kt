package com.gitlab.aecsocket.sokol.core

import com.gitlab.aecsocket.alexandria.core.extension.force
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

    fun componentType(key: Key): ComponentType<*>?
}

class PersistenceException(message: String? = null, cause: Throwable? = null)
    : RuntimeException(message, cause)

fun interface PersistenceMacro {
    fun apply(blueprint: EntityBlueprint, node: ConfigurationNode, profile: EntityProfile)
}

abstract class SokolPersistence(protected val sokol: SokolAPI) {
    private lateinit var mProfiled: ComponentMapper<Profiled>
    private val macros = ArrayList<PersistenceMacro>()

    open fun enable() {
        mProfiled = sokol.engine.mapper()
    }

    fun macro(macro: PersistenceMacro) {
        macros.add(macro)
    }

    abstract fun tagContext(): NBTTagContext

    fun blueprintOf(entity: SokolEntity): EntityBlueprint {
        return readBlueprint(writeEntity(entity))
    }

    private fun setProfile(blueprint: EntityBlueprint, profile: EntityProfile) {
        if (profile is KeyedEntityProfile)
            blueprint.pushSet(mProfiled) { Profiled(profile) }
    }

    fun emptyBlueprint(profile: EntityProfile): EntityBlueprint {
        val blueprint = sokol.engine.newBlueprint()

        fun <C : SokolComponent> push(profile: ComponentProfile<C>) {
            blueprint.pushSet(profile, profile.createEmpty())
        }

        profile.components.forEach { (_, profile) -> push(profile) }
        setProfile(blueprint, profile)

        return blueprint
    }

    fun readProfiledBlueprint(tag: CompoundNBTTag, profile: EntityProfile): EntityBlueprint {
        val blueprint = sokol.engine.newBlueprint()

        blueprint.flags = tag.getOr(FLAGS) { asInt() } ?: 0

        fun <C : SokolComponent> push(key: Key, profile: ComponentProfile<C>) {
            val component = try {
                tag[key.asString()]?.let { config ->
                    profile.read(config)
                } ?: profile.createEmpty()
            } catch (ex: PersistenceException) {
                throw PersistenceException("Could not read component $key", ex)
            }

            blueprint.pushSet(profile, component)
        }

        profile.components.forEach { (key, profile) -> push(key, profile) }
        setProfile(blueprint, profile)

        return blueprint
    }

    fun readBlueprint(tag: CompoundNBTTag): EntityBlueprint {
        val profileId = tag.get(PROFILE) { asString() }
        val entityProfile = sokol.entityProfile(profileId)
            ?: throw PersistenceException("Invalid entity profile '$profileId'")

        return readProfiledBlueprint(tag, entityProfile)
    }

    fun deserializeProfiledBlueprint(node: ConfigurationNode, profile: EntityProfile): EntityBlueprint {
        val blueprint = sokol.engine.newBlueprint()

        macros.forEach { it.apply(blueprint, node, profile) }

        blueprint.flags = node.node(FLAGS).get { 0 }

        fun <C : SokolComponent> push(key: Key, profile: ComponentProfile<C>) {
            val config = node.node(key.asString())
            val component = try {
                if (config.empty()) profile.createEmpty()
                else profile.deserialize(config)
            } catch (ex: SerializationException) {
                throw SerializationException(config, SokolEntity::class.java, "Could not read component $key", ex)
            }

            blueprint.pushSet(profile, component)
        }

        profile.components.forEach { (key, profile) -> push(key, profile) }
        setProfile(blueprint, profile)

        return blueprint
    }

    fun deserializeBlueprint(node: ConfigurationNode): EntityBlueprint {
        val profileId = (if (node.isMap) node.node(PROFILE) else node).force<String>()
        val entityProfile = sokol.entityProfile(profileId)
            ?: throw PersistenceException("Invalid entity profile '$profileId'")

        return deserializeProfiledBlueprint(node, entityProfile)
    }

    fun writeEntity(entity: SokolEntity, tag: CompoundNBTTag = tagContext().makeCompound()): CompoundNBTTag {
        tag.set(FLAGS) { makeInt(entity.flags) }

        entity.components.forEach { component ->
            when (component) {
                is Profiled -> tag.set(PROFILE) { makeString(component.profile.id) }
                is PersistentComponent -> {
                    try {
                        component.write(tagContext())?.let { tag.set(component.key.asString(), it) }
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
                    (tag[sKey]?.let { component.writeDelta(it) } ?: component.write(tag))?.let { tag[sKey] = it }
                    component.clean()
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
