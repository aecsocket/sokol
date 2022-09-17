package com.gitlab.aecsocket.sokol.core

import net.kyori.adventure.key.Key
import org.spongepowered.configurate.ConfigurationNode

// inspired by https://github.com/libgdx/ashley

interface SokolHost

interface SokolEntity {
    val components: Map<Key, SokolComponent>
    val host: SokolHost

    fun addComponent(component: SokolComponent)

    fun removeComponent(key: Key)
}

interface SokolComponent {
    val type: SokolComponentType

    fun serialize(tag: CompoundNBTTag.Mutable)

    fun serialize(node: ConfigurationNode)
}

interface SokolComponentType {
    val key: Key

    fun deserialize(tag: CompoundNBTTag): SokolComponent

    fun deserialize(node: ConfigurationNode): SokolComponent
}

fun interface SokolSystem {
    fun update()
}
