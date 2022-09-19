package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.sokol.core.CompoundNBTTag
import com.gitlab.aecsocket.sokol.core.SokolComponent
import net.kyori.adventure.key.Key
import org.spongepowered.configurate.ConfigurationNode

interface PersistentComponent : SokolComponent {
    val key: Key

    fun write(tag: CompoundNBTTag.Mutable)

    fun write(node: ConfigurationNode)
}

interface PersistentComponentType {
    val key: Key

    fun read(tag: CompoundNBTTag): PersistentComponent

    fun read(node: ConfigurationNode): PersistentComponent
}
