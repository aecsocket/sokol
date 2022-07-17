package com.gitlab.aecsocket.sokol.core

import com.gitlab.aecsocket.sokol.core.nbt.TagSerializable

fun <N : DataNode> NodeKey<N>.slot() = node.component.slots[key]

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

        fun copy(
            component: C = this.component,
            features: Map<String, F> = this.features,
            parent: NodeKey<N>? = this.parent,
            children: Map<String, N> = this.children,
        ): N
    }
}

fun DataNode.errorMsg(msg: String) = "${path()}/${component.id}: $msg"
