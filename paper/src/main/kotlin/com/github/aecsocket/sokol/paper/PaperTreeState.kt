package com.github.aecsocket.sokol.paper

import com.github.aecsocket.sokol.core.DataNode
import com.github.aecsocket.sokol.core.NodePath
import com.github.aecsocket.sokol.core.event.NodeEvent
import com.github.aecsocket.sokol.core.impl.AbstractTreeState
import com.github.aecsocket.sokol.core.stat.StatMap

class PaperTreeState(
    root: PaperDataNode,
    stats: StatMap,
    nodeStates: Map<PaperDataNode, Map<String, PaperFeature.State>>,
    val incomplete: List<NodePath>
) : AbstractTreeState<PaperTreeState, PaperDataNode, PaperNodeHost, PaperFeature.Data, PaperFeature.State>(
    root, stats, nodeStates
) {
    override val self: PaperTreeState
        get() = this

    override fun <E : NodeEvent> callEvent(host: PaperNodeHost, event: E): E {
        nodeStates.forEach { (node, states) ->
            val ctx = object : PaperFeatureContext {
                override val state: PaperTreeState
                    get() = this@PaperTreeState
                override val host: PaperNodeHost
                    get() = host
                override val node: DataNode
                    get() = node

                override fun writeNode(action: PaperDataNode.() -> Unit) {
                    TODO("Not yet implemented")
                }
            }
            states.forEach { (_, state) ->
                state.onEvent(event, ctx)
            }
        }
        return event
    }
}
