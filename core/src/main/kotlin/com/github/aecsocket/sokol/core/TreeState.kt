package com.github.aecsocket.sokol.core

import com.github.aecsocket.sokol.core.event.NodeEvent
import com.github.aecsocket.sokol.core.stat.StatMap

interface NodeHost

open class StateBuildException(message: String? = null, cause: Throwable? = null)
    : RuntimeException(message, cause)

interface TreeState {
    val root: DataNode
    val stats: StatMap

    interface Scoped<
        S : Scoped<S, N, H>,
        N : DataNode,
        H : NodeHost
    > : TreeState {
        val self: S
        override val root: N
        override val stats: StatMap

        fun <E : NodeEvent<S>> callEvent(host: H, factory: (S) -> E): E
    }
}
