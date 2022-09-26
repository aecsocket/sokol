package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.sokol.core.CompoundNBTTag
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

fun PersistentComponent.writeKeyed(tag: CompoundNBTTag.Mutable) =
    tag.set(key.toString(), ::write)

fun PersistentComponent.writeKeyed(node: ConfigurationNode) =
    write(node.node(key.toString()))

interface PersistentComponentType {
    val key: Key

    fun read(tag: NBTTag): PersistentComponent

    fun read(node: ConfigurationNode): PersistentComponent

    fun readFactory(node: ConfigurationNode): PersistentComponentFactory
}
