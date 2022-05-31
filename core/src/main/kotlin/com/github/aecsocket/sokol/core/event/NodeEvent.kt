package com.github.aecsocket.sokol.core.event

import com.github.aecsocket.sokol.core.TreeState

interface NodeEvent<S : TreeState.Scoped<S, *, *>> {
    val state: S

    data class Tick<S : TreeState.Scoped<S, *, *>>(
        override val state: S
    ) : NodeEvent<S>
}
