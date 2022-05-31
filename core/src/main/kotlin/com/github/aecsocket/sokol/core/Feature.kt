package com.github.aecsocket.sokol.core

import com.github.aecsocket.alexandria.core.EventDispatcher
import com.github.aecsocket.alexandria.core.keyed.Keyed
import com.github.aecsocket.sokol.core.event.NodeEvent
import org.spongepowered.configurate.ConfigurationNode

interface Feature<
    N : DataNode<*>,
    P : Feature.Profile<*>
> : Keyed {
    fun deserialize(node: ConfigurationNode): P

    interface Profile<D : Data<*>> {
        fun createData(): D

        fun deserialize(node: ConfigurationNode): D
    }

    interface Data<S : State<*, *>> {
        fun createState(): S

        fun serialize(node: ConfigurationNode)
    }

    interface State<F : State<F, S>, S : TreeState.Scoped<S, *, *>> {
        fun setUp(
            events: EventDispatcher.Builder<NodeEvent<S>>,
            node: TreeState.NodeState<F>
        )
    }
}
