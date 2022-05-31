package com.github.aecsocket.sokol.core

import com.github.aecsocket.alexandria.core.EventDispatcher
import com.github.aecsocket.sokol.core.event.NodeEvent
import com.github.aecsocket.sokol.core.stat.StatMap

open class StateBuildException(message: String? = null, cause: Throwable? = null)
    : RuntimeException(message, cause)

interface NodeHost {

}

interface TreeState {
    val root: DataNode
    val stats: StatMap
    val host: NodeHost

    interface Scoped<
        S : Scoped<S, N, H>,
        N : DataNode,
        H : NodeHost
    > : TreeState {
        val self: S
        override val root: N
        override val stats: StatMap
        override val host: H

        fun <E : NodeEvent<S>> callEvent(factory: (S) -> E): E
    }

    data class BuildInfo<
        S : Scoped<S, N, H>,
        N : DataNode,
        H : NodeHost
    >(
        val stats: StatMap,
        val events: EventDispatcher<NodeEvent<S>>
    )

    data class NodeState<S : Feature.State<S, *>>(
        private val features: Map<String, S>
    ) {
        fun feature(id: String) = features[id]
    }

    companion object {
        fun <
            T : Scoped<T, N, H>,
            N : DataNode.Scoped<N, C, D, T>,
            H : NodeHost,
            C : NodeComponent.Scoped<C, P, *>,
            P : Feature.Profile<D>,
            D : Feature.Data<S>,
            S : Feature.State<S, T>
        > from(root: N, walk: (NodePath, N) -> Unit): BuildInfo<T, N, H> {
            val stats = object : StatMap {} // todo

            val events = EventDispatcher.builder<NodeEvent<T>>()

            root.walk { path, child ->
                val childFeatures = HashMap<String, S>()
                child.value.features.forEach { (key, profile) ->
                    childFeatures[key] = (child.features[key] ?: profile.createData()).createState()
                }
                val state = NodeState(childFeatures)
                childFeatures.forEach { (_, feature) ->
                    feature.setUp(events, state)
                }

                walk(path, child)
            }

            return BuildInfo(stats, events.build())
        }
    }
}
