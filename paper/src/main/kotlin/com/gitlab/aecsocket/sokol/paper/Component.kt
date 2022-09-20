package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.sokol.core.NBTTag
import com.gitlab.aecsocket.sokol.core.SokolComponent
import net.kyori.adventure.key.Key
import org.spongepowered.configurate.ConfigurationNode

typealias NBTWriter = NBTTag.() -> NBTTag

interface PersistentComponent : SokolComponent {
    val key: Key

    fun write(): NBTWriter

    fun write(node: ConfigurationNode)
}

interface PersistentComponentType {
    val key: Key

    fun read(tag: NBTTag): PersistentComponent

    fun read(node: ConfigurationNode): PersistentComponent
}
