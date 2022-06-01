package com.github.aecsocket.sokol.core.impl

import com.github.aecsocket.sokol.core.*
import com.github.aecsocket.sokol.core.stat.StatMap

abstract class AbstractTreeState<
    S : AbstractTreeState<S, N, H, F>,
    N,
    H : NodeHost,
    F : Feature.State<F, N, H, S>
>(
    override val root: N,
    override val stats: StatMap
) : TreeState.Scoped<S, N, H> where N : DataNode.Scoped<N, *, *, *>, N : Node.Mutable<N>
