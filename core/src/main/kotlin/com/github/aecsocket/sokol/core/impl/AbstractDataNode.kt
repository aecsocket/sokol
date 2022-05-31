package com.github.aecsocket.sokol.core.impl

import com.github.aecsocket.sokol.core.*

abstract class AbstractDataNode<
    N : AbstractDataNode<N, C, F, S>,
    C : NodeComponent,
    F : Feature.Data<*>,
    S : TreeState<S, N, *>
>(
    override val value: C,
    override val features: MutableMap<String, F> = HashMap(),
    parent: NodeKey<N>?,
    children: MutableMap<String, N>
) : AbstractNode<N>(parent, children), DataNode.Scoped<N, C, F, S>
