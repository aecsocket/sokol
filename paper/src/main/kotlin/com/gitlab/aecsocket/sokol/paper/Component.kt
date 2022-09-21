package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.sokol.core.NBTTag
import com.gitlab.aecsocket.sokol.core.NBTTagContext
import com.gitlab.aecsocket.sokol.core.SokolComponent
import net.kyori.adventure.key.Key
import org.spongepowered.configurate.ConfigurationNode

interface PersistentComponent : SokolComponent {
    val key: Key

    fun write(ctx: NBTTagContext): NBTTag

    fun write(node: ConfigurationNode)
}

interface PersistentComponentType {
    val key: Key

    fun read(tag: NBTTag): PersistentComponent

    fun read(node: ConfigurationNode): PersistentComponent
}
