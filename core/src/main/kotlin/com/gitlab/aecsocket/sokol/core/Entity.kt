package com.gitlab.aecsocket.sokol.core

import com.gitlab.aecsocket.alexandria.core.keyed.Keyed
import net.kyori.adventure.key.Key

interface EntityProfile {
    val componentProfiles: Map<Key, ComponentProfile>

    fun componentProfile(key: Key): ComponentProfile?
}

open class SimpleEntityProfile(
    override val componentProfiles: Map<Key, ComponentProfile>
) : EntityProfile {
    override fun componentProfile(key: Key) = componentProfiles[key]
}

class KeyedEntityProfile(
    override val id: String,
    componentProfiles: Map<Key, ComponentProfile>
) : SimpleEntityProfile(componentProfiles), Keyed

open class EntityBlueprint(
    open val profile: EntityProfile,
    val components: MutableComponentMap
) {
    open fun copyOf() = EntityBlueprint(profile, components.mutableCopy())
}

class KeyedEntityBlueprint(
    override val profile: KeyedEntityProfile,
    components: MutableComponentMap
) : EntityBlueprint(profile, components) {
    override fun copyOf() = KeyedEntityBlueprint(profile, components.mutableCopy())
}

interface SokolEntity {
    val profile: EntityProfile
    val components: MutableComponentMap

    fun <E : SokolEvent> call(event: E): E
}

fun SokolEntity.toBlueprint() = EntityBlueprint(profile, components.mutableCopy())
