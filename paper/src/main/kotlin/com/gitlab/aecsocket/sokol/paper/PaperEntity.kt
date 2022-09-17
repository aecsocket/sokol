package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.sokol.core.SokolComponent
import com.gitlab.aecsocket.sokol.core.SokolEntity
import com.gitlab.aecsocket.sokol.core.SokolHost
import net.kyori.adventure.key.Key

class PaperEntity(
    override val host: SokolHost,
) : SokolEntity {
    private val _components = HashMap<Key, SokolComponent>()
    override val components: Map<Key, SokolComponent> get() = _components

    override fun addComponent(component: SokolComponent) {
        _components[component.type.key] = component
    }

    override fun removeComponent(key: Key) {
        _components.remove(key)
    }
}
