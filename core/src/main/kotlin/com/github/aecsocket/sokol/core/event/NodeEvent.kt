package com.github.aecsocket.sokol.core.event

import com.github.aecsocket.alexandria.core.Input
import com.github.aecsocket.sokol.core.TreeState

interface NodeEvent<S : TreeState.Scoped<S, *, *>> {
    val state: S

    data class OnTick<S : TreeState.Scoped<S, *, *>>(
        override val state: S
    ) : NodeEvent<S>

    data class OnInput<S : TreeState.Scoped<S, *, *>>(
        override val state: S,
        val input: Input
    ) : NodeEvent<S>
}
