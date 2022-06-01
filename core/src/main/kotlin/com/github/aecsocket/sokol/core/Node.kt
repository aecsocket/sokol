package com.github.aecsocket.sokol.core

data class NodeKey<out N : Node>(val node: N, val key: String)

interface NodePath : Iterable<String> {
    val size: Int

    operator fun get(index: Int): String?
    operator fun plus(node: String): NodePath
    operator fun plus(nodes: List<String>): NodePath
    operator fun plus(nodes: NodePath): NodePath

    companion object {
        val EMPTY: NodePath = EmptyNodePath

        fun of(nodes: List<String>): NodePath = NodePathImpl(nodes)

        fun of(nodes: Iterable<String>): NodePath = NodePathImpl(nodes.toList())

        fun of(vararg nodes: String): NodePath = NodePathImpl(nodes.asList())
    }
}

private object EmptyNodePath : NodePath {
    override val size: Int
        get() = 0

    override fun get(index: Int) = null
    override fun plus(node: String) = NodePath.of(node)
    override fun plus(nodes: List<String>) = NodePath.of(nodes)
    override fun plus(nodes: NodePath) = nodes

    override fun iterator() = object : Iterator<String> {
        override fun hasNext() = false
        override fun next() = throw NoSuchElementException()
    }
}

private data class NodePathImpl(
    val nodes: List<String>
) : NodePath {
    override val size: Int
        get() = nodes.size

    override fun get(index: Int) = nodes[index]
    override fun plus(node: String) = NodePath.of(nodes + node)
    override fun plus(nodes: List<String>) = NodePath.of(this.nodes + nodes)
    override fun plus(nodes: NodePath) = NodePath.of(this.nodes + nodes)

    override fun iterator() = nodes.iterator()
}

interface Node {
    val parent: NodeKey<Node>?
    val children: Map<String, Node>
    fun node(key: String): Node?
    fun node(path: Iterable<String>): Node?
    fun node(vararg path: String): Node?
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
        override fun node(key: String): N?
        override fun node(path: Iterable<String>): N?
        override fun node(vararg path: String): N?
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

fun <N : Node> N.keyOf(key: String) = NodeKey(this, key)
