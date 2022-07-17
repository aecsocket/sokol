package com.gitlab.aecsocket.sokol.core.impl

import com.gitlab.aecsocket.sokol.core.*

abstract class AbstractDataNode<
    N : AbstractDataNode<N, C, F, S>,
    C : NodeComponent,
    F : Feature.Data<*>,
    S : TreeState.Scoped<S, N, *>
>(
    override val component: C,
    override val features: MutableMap<String, F> = HashMap(),
    parent: NodeKey<N>?,
    children: MutableMap<String, N>
) : AbstractNode<N>(parent, children), DataNode.Scoped<N, C, F, S> {
    override fun toString() = "${component.id}[$features]${super.toString()}"

    override fun walkDataNodes(action: (DataNode, NodePath) -> WalkResult) = walk(action)
}
