package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.core.extension.force
import com.gitlab.aecsocket.alexandria.core.keyed.Keyed
import com.gitlab.aecsocket.alexandria.core.keyed.Registry
import com.gitlab.aecsocket.alexandria.core.keyed.parseNodeAlexandriaKey
import com.gitlab.aecsocket.sokol.core.CompoundNBTTag
import com.gitlab.aecsocket.sokol.core.NBTTag
import com.gitlab.aecsocket.sokol.core.NBTTagContext
import com.gitlab.aecsocket.sokol.core.SokolComponent
import io.leangen.geantyref.TypeToken
import net.kyori.adventure.key.Key
import org.spongepowered.configurate.ConfigurationNode
import kotlin.reflect.KClass

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

abstract class RegistryComponentType<C : Keyed>(
    type: KClass<C>,
    private val configPath: String,
) : PersistentComponentType {
    private val type = type.java
    private val typeName = type.simpleName

    val registry = Registry.create<C>()

    fun entry(id: String) = registry[id]
        ?: throw IllegalArgumentException("Invalid $typeName config '$id'")

    fun load(node: ConfigurationNode) {
        node.node(configPath).childrenMap().forEach { (_, child) ->
            parseNodeAlexandriaKey(type, child)
            registry.register(child.force(TypeToken.get(type)))
        }
    }
}
