package com.github.aecsocket.sokol.core

interface DataNode : Node {
    override val parent: NodeKey<DataNode>?
    override val children: Map<String, DataNode>
    override operator fun get(key: String): DataNode?
    override operator fun get(path: Iterable<String>): DataNode?
    override fun get(vararg path: String): DataNode?

    val value: NodeComponent
    val features: Map<String, Feature.Data<*>>

    fun createState(): TreeState<*, *, *>

    interface Scoped<
        N : Scoped<N, C, F, S>,
        C : NodeComponent,
        F : Feature.Data<*>,
        S : TreeState<S, N, *>
    > : DataNode, Node.Scoped<N> {
        override val value: C
        override val features: Map<String, F>
        override fun createState(): S
    }
}
