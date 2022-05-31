package com.github.aecsocket.sokol.core

data class NodeKey<out N : Node>(val node: N, val key: String)

data class NodePath(private val nodes: List<String>) : Iterable<String> {
    val size = nodes.size

    operator fun get(index: Int) = nodes[index]
    operator fun plus(node: String) = NodePath(nodes + node)
    operator fun plus(nodes: List<String>) = NodePath(this.nodes + nodes)
    operator fun plus(nodes: NodePath) = NodePath(this.nodes + nodes.nodes)
    override fun iterator() = nodes.iterator()

    companion object {
        @JvmStatic val EMPTY = NodePath(emptyList())
    }
}

interface Node {
    val parent: NodeKey<Node>?
    val children: Map<String, Node>
    operator fun get(key: String): Node?
    operator fun get(path: Iterable<String>): Node?
    operator fun get(vararg path: String): Node?
    fun has(key: String): Boolean

    fun path(): NodePath
    fun root(): Node
    fun isRoot() = parent == null

    fun copy(): Node
    fun asRoot(): Node

    interface Scoped<N : Scoped<N>> : Node {
        val self: N
        override val parent: NodeKey<N>?
        override val children: Map<String, N>
        override operator fun get(key: String): N?
        override operator fun get(path: Iterable<String>): N?
        override operator fun get(vararg path: String): N?
        override fun root(): N
        override fun copy(): N
        override fun asRoot(): N

        fun walk(action: (NodePath, N) -> Unit)
    }

    interface Mutable<N : Mutable<N>> : Scoped<N> {
        fun detach()
        fun attach(node: N, key: String)

        fun remove(key: String)
        operator fun set(key: String, value: N)
    }
}
