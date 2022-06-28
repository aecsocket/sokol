package com.github.aecsocket.sokol.core

import com.github.aecsocket.sokol.core.event.NodeEvent
import com.github.aecsocket.sokol.core.stat.CompiledStatMap

interface NodeHost

open class StateBuildException(message: String? = null, cause: Throwable? = null)
    : RuntimeException(message, cause)

interface TreeState {
    val root: DataNode
    val stats: CompiledStatMap

    fun updatedRoot(): DataNode

    interface Scoped<
        S : Scoped<S, N, H>,
        N,
        H : NodeHost
    > : TreeState where N : DataNode, N : Node.Mutable<N> {
        val self: S
        override val root: N
        override val stats: CompiledStatMap

        override fun updatedRoot(): N

        fun <E : NodeEvent> callEvent(host: H, event: E): E
    }
}
