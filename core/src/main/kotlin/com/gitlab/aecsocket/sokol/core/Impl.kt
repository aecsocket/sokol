package com.gitlab.aecsocket.sokol.core

import net.kyori.adventure.key.Key

internal class EntityImpl(
    override val components: MutableMap<String, SokolComponent> = HashMap()
) : SokolEntity {
    override fun get(key: Key) = components[key.asString()]

    override fun add(component: SokolComponent) {
        components[component.key.asString()] = component
    }

    override fun remove(key: Key) {
        components.remove(key.asString())
    }
}
