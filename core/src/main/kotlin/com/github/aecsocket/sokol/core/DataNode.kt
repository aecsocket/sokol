package com.github.aecsocket.sokol.core

import com.github.aecsocket.sokol.core.nbt.TagSerializable

interface DataNode : Node, TagSerializable {
    override val parent: NodeKey<DataNode>?
    override val children: Map<String, DataNode>
    override fun node(key: String): DataNode?
    override fun node(path: Iterable<String>): DataNode?
    override fun node(vararg path: String): DataNode?
    override fun root(): DataNode

    val component: NodeComponent
    val features: Map<String, Feature.Data<*>>

    fun walkDataNodes(action: (DataNode, NodePath) -> WalkResult): Boolean

    interface Scoped<
        N : Scoped<N, C, F, S>,
        C : NodeComponent,
        F : Feature.Data<*>,
        S : TreeState
    > : DataNode, Node.Scoped<N> {
        override val component: C
        override val features: Map<String, F>
    }
}

fun DataNode.errorMsg(msg: String) = "${path()}/${component.id}: $msg"
