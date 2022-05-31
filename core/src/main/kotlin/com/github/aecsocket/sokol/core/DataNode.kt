package com.github.aecsocket.sokol.core

interface DataNode<H : NodeHost> : Node {
    override val parent: NodeKey<DataNode<H>>?
    override val children: Map<String, DataNode<H>>
    override operator fun get(key: String): DataNode<H>?
    override operator fun get(path: Iterable<String>): DataNode<H>?
    override fun get(vararg path: String): DataNode<H>?

    val value: NodeComponent
    val features: Map<String, Feature.Data<*>>

    fun createState(host: H): TreeState.Scoped<*, *, H>

    interface Scoped<
        N : Scoped<N, H, C, F, S>,
        H : NodeHost,
        C : NodeComponent,
        F : Feature.Data<*>,
        S : TreeState.Scoped<S, N, H>
    > : DataNode<H>, Node.Scoped<N> {
        override val value: C
        override val features: Map<String, F>
        override fun createState(host: H): S
    }
}
