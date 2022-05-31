package com.github.aecsocket.sokol.core.impl

import com.github.aecsocket.sokol.core.*

abstract class AbstractDataNode<
    N : AbstractDataNode<N, H, C, F, S>,
    H : NodeHost,
    C : NodeComponent,
    F : Feature.Data<*>,
    S : TreeState.Scoped<S, N, H>
>(
    override val value: C,
    override val features: MutableMap<String, F> = HashMap(),
    parent: NodeKey<N>?,
    children: MutableMap<String, N>
) : AbstractNode<N>(parent, children), DataNode.Scoped<N, H, C, F, S>
