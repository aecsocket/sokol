package com.github.aecsocket.sokol.core.impl

import com.github.aecsocket.sokol.core.*

abstract class AbstractNode<N : AbstractNode<N>>(
    override var parent: NodeKey<N>? = null,
    override val children: MutableMap<String, N> = HashMap(),
) : Node.Mutable<N> {
    override fun node(key: String) = children[key]
    override fun node(path: Iterable<String>): N? {
        var cur = self
        path.forEach { cur = cur.node(it) ?: return null }
        return cur
    }
    override fun node(vararg path: String) = node(path.asIterable())

    override fun has(key: String) = children.containsKey(key)
    override fun has(path: Iterable<String>) = node(path) != null
    override fun has(vararg path: String) = node(*path) != null

    override fun detach() {
        parent = null
    }

    override fun attach(node: N, key: String) {
        parent = node.keyOf(key)
    }

    override fun remove(key: String) {
        children.remove(key)
    }

    override fun set(key: String, value: N) {
        value.attach(self, key)
        children[key]?.detach()
        children[key] = value
    }

    override fun path(): NodePath = parent?.let { it.node.path() + it.key } ?: emptyNodePath()
    override fun root(): N = parent?.node?.root() ?: self
    override fun isRoot() = parent == null

    override fun asRoot(): N = copy().apply {
        detach()
    }

    private fun walk(path: NodePath, action: (NodePath, N) -> WalkResult): Boolean {
        return when (action(path, self)) {
            WalkResult.CONTINUE -> children.all { (key, child) -> child.walk(path + key, action) }
            WalkResult.STOP_BRANCH -> true
            WalkResult.STOP_ALL -> false
        }
    }

    override fun walk(action: (NodePath, N) -> WalkResult) = walk(emptyNodePath(), action)

    override fun walkNodes(action: (NodePath, Node) -> WalkResult) = walk(action)

    override fun toString() = children.toString()
}
