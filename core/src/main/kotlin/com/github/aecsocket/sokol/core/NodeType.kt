package com.github.aecsocket.sokol.core

import com.github.aecsocket.alexandria.core.EventDispatcher
import com.github.aecsocket.sokol.core.event.NodeEvent
import com.github.aecsocket.sokol.core.stat.StatMap

typealias NodeEventDispatcher = EventDispatcher<NodeEvent>

interface DataNode : Node {
    override val parent: NodeKey<DataNode>?
    override val children: Map<String, DataNode>
    override operator fun get(key: String): DataNode?
    override operator fun get(path: Iterable<String>): DataNode?
    override fun get(vararg path: String): DataNode?

    val value: NodeComponent
    val features: Map<String, Feature.Data<*>>

    interface Scoped<
        N : Scoped<N, C, F>,
        C : NodeComponent,
        F : Feature.Data<*>
    > : DataNode, Node.Scoped<N> {
        override val value: C
        override val features: Map<String, F>
    }
}

open class StateBuildException(message: String? = null, cause: Throwable? = null)
    : RuntimeException(message, cause)

class TreeState<
    N : DataNode.Scoped<N, *, D>,
    D : Feature.Data<S>,
    S : Feature.State<S>
> private constructor(
    val root: N,
    val stats: StatMap,
    private val events: NodeEventDispatcher,
    private val features: Map<NodePath, NodeState<S>>
) {
    data class NodeState<S : Feature.State<S>>(
        private val features: Map<String, S>
    ) {
        fun feature(id: String) = features[id]
    }

    fun <F : NodeEvent> callEvent(factory: (TreeState<N, D, S>) -> F) = events.call(factory(this))

    companion object {
        fun <
            N : DataNode.Scoped<N, C, D>,
            C : NodeComponent.Scoped<C, P, *>,
            P : Feature.Profile<D>,
            D : Feature.Data<S>,
            S : Feature.State<S>
        > from(root: N): TreeState<N, D, S> {
            val stats = object : StatMap {} // todo

            val events = EventDispatcher.builder<NodeEvent>()

            val features = HashMap<NodePath, NodeState<S>>()
            root.walk { path, child ->
                //println("[state] got node @ $path |${child.features.size}|")
                val childFeatures = HashMap<String, S>()

                child.value.features.forEach { (key, profile) ->
                    val state = (child.features[key] ?: profile.createData()).createState()
                    state.registerEvents(events)
                    childFeatures[key] = state
                }

                val state = NodeState(childFeatures)
                childFeatures.forEach { (_, feature) ->
                    feature.setUpState(state)
                }
            }

            //println("[state] events listeners = ${events.build().size}")
            return TreeState(root, stats, events.build(), features)
        }
    }
}
