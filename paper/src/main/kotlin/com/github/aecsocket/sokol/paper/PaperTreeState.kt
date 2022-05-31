package com.github.aecsocket.sokol.paper

import com.github.aecsocket.alexandria.core.EventDispatcher
import com.github.aecsocket.sokol.core.NodePath
import com.github.aecsocket.sokol.core.TreeState
import com.github.aecsocket.sokol.core.event.NodeEvent
import com.github.aecsocket.sokol.core.impl.AbstractTreeState
import com.github.aecsocket.sokol.core.stat.StatMap

typealias PaperNodeEvent = NodeEvent<PaperTreeState>

class PaperTreeState(
    root: PaperDataNode,
    stats: StatMap,
    host: PaperNodeHost,
    events: EventDispatcher<PaperNodeEvent>,
    val incomplete: List<NodePath>
) : AbstractTreeState<PaperTreeState, PaperDataNode, PaperNodeHost>(root, stats, host, events) {
    override val self: PaperTreeState
        get() = this

    companion object {
        fun from(root: PaperDataNode, host: PaperNodeHost): PaperTreeState {
            val incomplete = ArrayList<NodePath>()
            val (stats, events) = TreeState.from(root) { path, child ->
                child.value.slots.forEach { (key, slot) ->
                    if (slot.required && !child.has(key)) {
                        incomplete.add(path)
                    }
                }
            }
            return PaperTreeState(root, stats, host, events, incomplete)
        }
    }
}
