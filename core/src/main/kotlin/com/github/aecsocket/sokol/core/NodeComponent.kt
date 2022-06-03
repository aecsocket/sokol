package com.github.aecsocket.sokol.core

import com.github.aecsocket.alexandria.core.keyed.Keyed
import com.github.aecsocket.glossa.core.Localizable
import net.kyori.adventure.text.Component

interface NodeComponent : Keyed, Localizable<Component> {
    val features: Map<String, Feature.Profile<*>>
    val slots: Map<String, Slot>
    val tags: Set<String>

    interface Scoped<
        C : Scoped<C, F, S>,
        F : Feature.Profile<*>,
        S : Slot
    > : NodeComponent {
        override val features: Map<String, F>
        override val slots: Map<String, S>
    }
}

interface Slot : Localizable<Component> {
    val key: String
    val tags: Set<String>
}
