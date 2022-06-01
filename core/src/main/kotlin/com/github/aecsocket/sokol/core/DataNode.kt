package com.github.aecsocket.sokol.core

import com.github.aecsocket.sokol.core.nbt.CompoundBinaryTag

interface DataNode : Node {
    override val parent: NodeKey<DataNode>?
    override val children: Map<String, DataNode>
    override fun node(key: String): DataNode?
    override fun node(path: Iterable<String>): DataNode?
    override fun node(vararg path: String): DataNode?

    val component: NodeComponent
    val features: Map<String, Feature.Data<*>>

    fun serialize(tag: CompoundBinaryTag.Mutable)

    interface Scoped<
        N : Scoped<N, C, F, S>,
        C : NodeComponent,
        F : Feature.Data<*>,
        S : TreeState.Scoped<S, N, *>
    > : DataNode, Node.Scoped<N> {
        override val component: C
        override val features: Map<String, F>
    }
}
