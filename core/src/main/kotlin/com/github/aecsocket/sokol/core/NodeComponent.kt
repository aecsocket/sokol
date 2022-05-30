package com.github.aecsocket.sokol.core

import com.github.aecsocket.alexandria.core.keyed.Keyed

interface NodeComponent : Keyed {
    val features: Map<String, Feature.Profile<*>>
    val slots: Map<String, Slot>

    interface Scoped<
        C : Scoped<C, F, S>,
        F : Feature.Profile<*>,
        S : Slot
    > : NodeComponent {
        override val features: Map<String, F>
        override val slots: Map<String, S>
    }
}

interface Slot {
    val key: String
    val tags: Set<String>
}
