package com.gitlab.aecsocket.sokol.core

import com.gitlab.aecsocket.glossa.core.force
import net.kyori.adventure.key.Key
import org.spongepowered.configurate.ConfigurationNode

interface PersistentComponent : SokolComponent {
    val key: Key
    val dirty: Boolean

    fun write(ctx: NBTTagContext): NBTTag

    fun writeDelta(tag: NBTTag): NBTTag

    fun write(node: ConfigurationNode)
}

interface SimplePersistentComponent : PersistentComponent {
    override val dirty get() = false

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()

    override fun writeDelta(tag: NBTTag) = tag

    override fun write(node: ConfigurationNode) {}
}

interface ComponentProfile {
    fun read(tag: NBTTag): PersistentComponent

    fun read(node: ConfigurationNode): PersistentComponent

    fun readEmpty(): PersistentComponent
}

fun interface SimpleComponentProfile : ComponentProfile {
    override fun read(tag: NBTTag) = readEmpty()

    override fun read(node: ConfigurationNode) = readEmpty()
}

interface ComponentType {
    val key: Key

    fun createProfile(node: ConfigurationNode): ComponentProfile

    companion object {
        fun singletonComponent(key: Key, component: PersistentComponent) = object : ComponentType {
            override val key get() = key

            override fun createProfile(node: ConfigurationNode) = SimpleComponentProfile { component }
        }

        fun singletonProfile(key: Key, profile: ComponentProfile) = object : ComponentType {
            override val key get() = key

            override fun createProfile(node: ConfigurationNode): ComponentProfile {
                return profile
            }
        }

        inline fun <reified P : ComponentProfile> deserializing(key: Key) = object : ComponentType {
            override val key get() = key

            override fun createProfile(node: ConfigurationNode): P {
                return node.force<P>()
            }
        }
    }
}
