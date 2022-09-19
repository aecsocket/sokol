package com.gitlab.aecsocket.sokol.core

import net.kyori.adventure.key.Key
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required

@ConfigSerializable
data class ItemDescriptor(
    @Required val key: Key,
    val damage: Int = 0,
    val modelData: Int = 0,
    val unbreakable: Boolean = false,
    val flags: List<String> = emptyList(),
)
