package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.sokol.core.CompoundNBTTag
import com.gitlab.aecsocket.sokol.core.SokolComponent
import net.kyori.adventure.key.Key
import org.spongepowered.configurate.ConfigurationNode

interface PersistentComponent : SokolComponent {
    val key: Key

    fun serialize(tag: CompoundNBTTag.Mutable)

    fun serialize(node: ConfigurationNode)
}

interface PersistentComponentType {
    val key: Key

    fun deserialize(tag: CompoundNBTTag): PersistentComponent

    fun deserialize(node: ConfigurationNode): PersistentComponent
}
