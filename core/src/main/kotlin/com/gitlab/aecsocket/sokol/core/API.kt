package com.gitlab.aecsocket.sokol.core

import net.kyori.adventure.key.Key
import org.spongepowered.configurate.ConfigurationNode

// inspired by https://github.com/libgdx/ashley

interface SokolEntity {
    val components: Map<String, SokolComponent>

    operator fun get(key: Key): SokolComponent?

    fun add(component: SokolComponent)

    fun remove(key: Key)
}

interface SokolComponent {
    val key: Key

    interface Persistent : SokolComponent {
        fun serialize(tag: CompoundNBTTag.Mutable)

        fun serialize(node: ConfigurationNode)
    }
}

interface ComponentKey<C : SokolComponent> {
    val key: Key
}

interface SokolComponentType {
    val key: Key

    fun deserialize(tag: CompoundNBTTag): SokolComponent.Persistent

    fun deserialize(node: ConfigurationNode): SokolComponent.Persistent
}

fun <C : SokolComponent, T : ComponentKey<C>> SokolEntity.get(type: T): C? {
    @Suppress("UNCHECKED_CAST")
    return get(type.key) as C?
}

fun <C : SokolComponent, T : ComponentKey<C>> SokolEntity.force(type: T): C {
    val key = type.key
    @Suppress("UNCHECKED_CAST")
    return get(key) as? C? ?: throw IllegalStateException("No component '$key'")
}

fun interface SokolSystem {
    fun handle(entities: EntityAccessor, event: SokolEvent)
}
