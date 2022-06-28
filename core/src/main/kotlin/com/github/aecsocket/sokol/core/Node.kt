package com.github.aecsocket.sokol.core

data class NodeKey<out N : Node>(val node: N, val key: String)

interface NodePath : Iterable<String> {
    val size: Int

    operator fun get(index: Int): String?
    operator fun plus(node: String): NodePath
    operator fun plus(nodes: List<String>): NodePath
    operator fun plus(nodes: NodePath): NodePath
}

private object EmptyNodePath : NodePath {
    override val size: Int
        get() = 0

    override fun get(index: Int) = null
    override fun plus(node: String) = nodePathOf(node)
    override fun plus(nodes: List<String>) = nodePathOf(nodes)
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
    override fun plus(node: String) = nodePathOf(nodes + node)
    override fun plus(nodes: List<String>) = nodePathOf(this.nodes + nodes)
    override fun plus(nodes: NodePath) = nodePathOf(this.nodes + nodes)

    override fun iterator() = nodes.iterator()
}

fun emptyNodePath(): NodePath = EmptyNodePath

fun nodePathOf(nodes: List<String>): NodePath = NodePathImpl(nodes)

fun nodePathOf(nodes: Iterable<String>): NodePath = NodePathImpl(nodes.toList())

fun nodePathOf(vararg nodes: String): NodePath = NodePathImpl(nodes.asList())

enum class WalkResult {
    CONTINUE,
    STOP_BRANCH,
    STOP_ALL
}

interface Node {
    val parent: NodeKey<Node>?
    val children: Map<String, Node>

    fun node(key: String): Node?
    fun node(path: Iterable<String>): Node?
    fun node(vararg path: String): Node?

    fun has(key: String): Boolean
    fun has(path: Iterable<String>): Boolean
    fun has(vararg path: String): Boolean

    fun path(): NodePath
    fun root(): Node
    fun isRoot(): Boolean

    fun copy(): Node
    fun asRoot(): Node

    fun walkNodes(action: (NodePath, Node) -> WalkResult): Boolean

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

        fun walk(action: (NodePath, N) -> WalkResult): Boolean
    }

    interface Mutable<N : Mutable<N>> : Scoped<N> {
        fun detach()
        fun attach(node: N, key: String)

        fun remove(key: String)
        operator fun set(key: String, value: N)
    }
}

fun <N : Node> N.keyOf(key: String) = NodeKey(this, key)
