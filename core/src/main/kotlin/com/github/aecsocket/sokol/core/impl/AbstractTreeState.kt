package com.github.aecsocket.sokol.core.impl

import com.github.aecsocket.alexandria.core.EventDispatcher
import com.github.aecsocket.sokol.core.DataNode
import com.github.aecsocket.sokol.core.NodeHost
import com.github.aecsocket.sokol.core.TreeState
import com.github.aecsocket.sokol.core.event.NodeEvent
import com.github.aecsocket.sokol.core.stat.StatMap

abstract class AbstractTreeState<
    S : AbstractTreeState<S, N, H>,
    N : DataNode<H>,
    H : NodeHost
>(
    override val root: N,
    override val stats: StatMap,
    override val host: H,
    private val events: EventDispatcher<NodeEvent<S>>
) : TreeState.Scoped<S, N, H> {
    override fun <E : NodeEvent<S>> callEvent(factory: (S) -> E) =
        events.call(factory(self))
}
