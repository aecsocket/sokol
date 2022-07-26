package com.gitlab.aecsocket.sokol.core.impl

import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.stat.CompiledStatMap

abstract class AbstractTreeState<
    S : AbstractTreeState<S, N, H, D, F>,
    N : AbstractDataNode<N, *, D, S>,
    H : NodeHost<N>,
    D : Feature.Data<F>,
    F : Feature.State<F, D, *>
>(
    override val root: N,
    override val stats: CompiledStatMap,
    val nodeStates: Map<N, Map<String, F>>,
) : TreeState.Scoped<S, N, H> {
    override fun updatedRoot(): N {
        nodeStates.forEach { (node, states) ->
            states.forEach { (key, state) ->
                node.features[key] = state.asData()
            }
        }
        return root
    }
}
