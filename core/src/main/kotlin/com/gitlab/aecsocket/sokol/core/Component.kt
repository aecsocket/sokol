package com.gitlab.aecsocket.sokol.core

import com.gitlab.aecsocket.glossa.core.force
import com.gitlab.aecsocket.sokol.core.util.Bits
import net.kyori.adventure.key.Key
import org.spongepowered.configurate.ConfigurationNode
import kotlin.reflect.KClass

class ComponentPersistenceException(messsage: String? = null, cause: Throwable? = null)
    : RuntimeException(messsage, cause)

interface SokolComponent {
    val componentType: KClass<out SokolComponent>
}

interface PersistentComponent : SokolComponent {
    val key: Key

    fun write(ctx: NBTTagContext): NBTTag

    fun write(node: ConfigurationNode)
}

interface ComponentProfile {
    fun read(tag: NBTTag): PersistentComponent

    fun read(node: ConfigurationNode): PersistentComponent

    fun readEmpty(): PersistentComponent
}

fun interface NonReadingComponentProfile : ComponentProfile {
    override fun read(tag: NBTTag) = readEmpty()

    override fun read(node: ConfigurationNode) = readEmpty()
}

interface ComponentType {
    val key: Key

    fun createProfile(node: ConfigurationNode): ComponentProfile

    companion object {
        fun singletonComponent(key: Key, component: PersistentComponent) = object : ComponentType {
            override val key get() = key

            override fun createProfile(node: ConfigurationNode) = object : ComponentProfile {
                override fun read(tag: NBTTag) = component

                override fun read(node: ConfigurationNode) = component

                override fun readEmpty() = component
            }
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

interface ComponentMap {
    fun archetype(): Bits

    fun all(): Collection<SokolComponent>

    fun has(id: Int): Boolean

    fun has(type: KClass<out SokolComponent>): Boolean

    fun get(id: Int): SokolComponent?

    fun <C : SokolComponent> get(type: KClass<out C>): C?

    fun mutableCopy(): MutableComponentMap
}

inline fun <reified C : SokolComponent> ComponentMap.has() = has(C::class)

inline fun <reified C : SokolComponent> ComponentMap.get() = get(C::class)

interface MutableComponentMap : ComponentMap {
    fun set(id: Int, component: SokolComponent)

    fun set(component: SokolComponent)

    fun removeById(id: Int)

    fun removeByType(type: KClass<out SokolComponent>)
}

inline fun <reified C : SokolComponent> MutableComponentMap.removeByType() = removeByType(C::class)

fun MutableComponentMap.removeByType(component: SokolComponent) = removeByType(component.componentType)
