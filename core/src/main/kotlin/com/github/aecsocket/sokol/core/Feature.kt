package com.github.aecsocket.sokol.core

import com.github.aecsocket.alexandria.core.keyed.Keyed
import com.github.aecsocket.sokol.core.event.NodeEvent
import com.github.aecsocket.sokol.core.nbt.CompoundBinaryTag
import com.github.aecsocket.sokol.core.nbt.TagSerializable
import org.spongepowered.configurate.ConfigurationNode

interface Feature<
    N : DataNode,
    P : Feature.Profile<*>
> : Keyed {
    fun createProfile(node: ConfigurationNode): P

    interface Profile<D : Data<*>> {
        fun createData(): D

        fun createData(node: ConfigurationNode): D

        fun createData(tag: CompoundBinaryTag): D
    }

    interface Data<S : State<S, *, *>> : TagSerializable {
        fun createState(): S

        fun serialize(node: ConfigurationNode)
    }

    interface State<
        S : State<S, D, C>,
        D : Data<S>,
        C : FeatureContext<*, *, *>
    > : TagSerializable {
        fun asData(): D

        fun resolveDependencies(get: (String) -> S?)

        fun onEvent(event: NodeEvent, ctx: C)
    }
}

interface FeatureContext<
    S : TreeState,
    H : NodeHost,
    N
> where N : DataNode, N : Node.Mutable<N> {
    val state: S
    val host: H
    val node: DataNode

    fun writeNode(action: N.() -> Unit)
}
