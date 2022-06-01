package com.github.aecsocket.sokol.core

import com.github.aecsocket.alexandria.core.keyed.Keyed
import com.github.aecsocket.sokol.core.event.NodeEvent
import com.github.aecsocket.sokol.core.nbt.CompoundBinaryTag
import org.spongepowered.configurate.ConfigurationNode

interface Feature<
    N : DataNode,
    P : Feature.Profile<*>
> : Keyed {
    fun createProfile(node: ConfigurationNode): P

    interface Profile<D : Data<*>> {
        fun createData(): D

        fun serialize(node: ConfigurationNode): D

        fun deserialize(tag: CompoundBinaryTag): D
    }

    interface Data<S : State<*, *, *, *>> {
        fun createState(): S

        fun serialize(node: ConfigurationNode)

        fun serialize(tag: CompoundBinaryTag.Mutable)
    }

    interface State<
        S : State<S, N, H, T>,
        N,
        H : NodeHost,
        T : TreeState.Scoped<T, N, H>
    > where N : DataNode, N : Node.Mutable<N> {
        fun resolveDependencies(get: (String) -> S?)

        fun onEvent(event: NodeEvent<T>, ctx: FeatureContext<N, H>)
    }
}

interface FeatureContext<N, H : NodeHost> where N : DataNode, N : Node.Mutable<N> {
    val host: H
    val tag: CompoundBinaryTag
    val node: DataNode

    fun writeTag(action: CompoundBinaryTag.Mutable.() -> Unit)

    fun writeNode(action: N.() -> Unit)
}
