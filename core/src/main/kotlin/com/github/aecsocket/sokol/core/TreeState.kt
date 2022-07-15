package com.github.aecsocket.sokol.core

import com.github.aecsocket.sokol.core.event.NodeEvent
import com.github.aecsocket.sokol.core.stat.ApplicableStats
import com.github.aecsocket.sokol.core.stat.CompiledStatMap
import com.github.aecsocket.sokol.core.stat.statMapOf

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

data class TreeStateData<
    N : DataNode,
    FS : Feature.State<FS, *, *>
>(
    val stats: CompiledStatMap,
    val nodeStates: Map<N, Map<String, FS>>,
    val incomplete: List<NodePath>
)

fun <
    C : NodeComponent.Scoped<C, FP, *>,
    FP : Feature.Profile<FD>,
    FD : Feature.Data<FS>,
    FS : Feature.State<FS, *, *>,
    N : DataNode.Scoped<N, C, FD, *>
> treeStateData(root: N): TreeStateData<N, FS> {
    data class NodeStat(val node: N, val stats: List<ApplicableStats>)

    val forwardStats = ArrayList<NodeStat>()
    val reverseStats = ArrayList<NodeStat>()
    val incomplete = ArrayList<NodePath>()
    val nodeStates = HashMap<N, Map<String, FS>>()

    root.walk { child, path ->
        val component = child.component

        // states
        val nodeState = HashMap<String, FS>()
        val featuresLeft = component.features.toMutableMap()
        child.features.forEach { (key, data) ->
            if (featuresLeft.remove(key) != null) {
                nodeState[key] = data.createState()
            }
        }
        // · if any features in the component weren't in the data, we init their state here
        featuresLeft.forEach { (key, profile) ->
            nodeState[key] = profile.createData().createState()
        }

        nodeStates[child] = nodeState

        // stats
        val stats = component.stats.toMutableList()
        nodeState.forEach { (_, state) ->
            state.resolveDependencies(nodeState::get)
            stats += state.createStats()
        }
        forwardStats.add(NodeStat(child, stats.filter { !it.reversed }))
        reverseStats.add(0, NodeStat(child, stats.filter { it.reversed }))

        // incomplete
        component.slots.forEach { (key, slot) ->
            if (slot.required && !child.has(key)) {
                incomplete.add(path + key)
            }
        }

        WalkResult.CONTINUE
    }

    val stats = statMapOf()
    fun add(nodeStats: List<NodeStat>) {
        nodeStats.forEach { (node, applicable) ->
            applicable.sortedBy { it.priority }.forEach {
                if (it.rule.applies(node)) {
                    stats.merge(it.stats)
                }
            }
        }
    }
    add(forwardStats)
    add(reverseStats)

    return TreeStateData(
        stats.compile(),
        nodeStates,
        incomplete
    )
}
