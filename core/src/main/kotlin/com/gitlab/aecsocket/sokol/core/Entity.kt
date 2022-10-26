package com.gitlab.aecsocket.sokol.core

import com.gitlab.aecsocket.alexandria.core.keyed.Keyed
import net.kyori.adventure.key.Key

data class EntityProfile(
    override val id: String,
    val componentProfiles: Map<Key, ComponentProfile>,
) : Keyed {
    fun componentProfile(key: Key) = componentProfiles[key]
}

data class EntityBlueprint(
    val profile: EntityProfile,
    val components: MutableComponentMap
) {
    fun copyOf() = EntityBlueprint(profile, components.mutableCopy())
}

interface SokolEntity {
    val profile: EntityProfile
    val components: MutableComponentMap

    fun <E : SokolEvent> call(event: E): E
}

fun SokolEntity.toBlueprint() = EntityBlueprint(profile, components.mutableCopy())
