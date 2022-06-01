package com.github.aecsocket.sokol.core.impl

import com.github.aecsocket.sokol.core.*
import com.github.aecsocket.sokol.core.nbt.CompoundBinaryTag
import com.github.aecsocket.sokol.core.stat.StatMap

abstract class AbstractTreeState<
    S : AbstractTreeState<S, N, H, D, F>,
    N : AbstractDataNode<N, *, D, S>,
    H : NodeHost,
    D : Feature.Data<F>,
    F : Feature.State<F, D, *>
>(
    override val root: N,
    override val stats: StatMap,
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
