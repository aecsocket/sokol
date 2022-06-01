package com.github.aecsocket.sokol.paper

import com.github.aecsocket.sokol.core.DataNode
import com.github.aecsocket.sokol.core.FeatureContext
import com.github.aecsocket.sokol.core.NodePath
import com.github.aecsocket.sokol.core.event.NodeEvent
import com.github.aecsocket.sokol.core.impl.AbstractTreeState
import com.github.aecsocket.sokol.core.nbt.CompoundBinaryTag
import com.github.aecsocket.sokol.core.stat.StatMap

data class NodeState(
    val tag: CompoundBinaryTag.Mutable,
    val features: Iterable<Pair<PaperFeature.State, CompoundBinaryTag.Mutable>>
)

class PaperTreeState(
    root: PaperDataNode,
    stats: StatMap,
    val nodeStates: Map<PaperDataNode, NodeState>,
    val incomplete: List<NodePath>
) : AbstractTreeState<PaperTreeState, PaperDataNode, PaperNodeHost, PaperFeature.State>(root, stats) {
    override val self: PaperTreeState
        get() = this

    override fun <E : NodeEvent<PaperTreeState>> callEvent(host: PaperNodeHost, factory: (PaperTreeState) -> E): E {
        val event = factory(this)
        println(" > node states = $nodeStates")
        nodeStates.forEach { (node, nodeState) ->
            nodeState.features.forEach { (state, tag) ->
                val tagWrites = ArrayList<CompoundBinaryTag.Mutable.() -> Unit>()
                val nodeWrites = ArrayList<PaperDataNode.() -> Unit>()

                val context = object : FeatureContext<PaperDataNode, PaperNodeHost> {
                    override val host: PaperNodeHost
                        get() = host
                    override val tag: CompoundBinaryTag
                        get() = tag
                    override val node: DataNode
                        get() = node

                    override fun writeTag(action: CompoundBinaryTag.Mutable.() -> Unit) {
                        tagWrites.add(action)
                    }

                    override fun writeNode(action: PaperDataNode.() -> Unit) {
                        nodeWrites.add(action)
                    }
                }

                println(" > state for evt = $state")
                state.onEvent(event, context)

                println(" > writes: ${nodeWrites.size} vs ${tagWrites.size} into $tag")
                if (nodeWrites.isEmpty()) {
                    // we've only touched our own feature's data
                    tagWrites.forEach { it(tag) }
                } else {
                    // discard the tag writes - they will be overwritten in node serialization anyway
                    nodeWrites.forEach { it(node) }
                    node.serialize(nodeState.tag)
                }
            }
        }
        return event
    }
}
