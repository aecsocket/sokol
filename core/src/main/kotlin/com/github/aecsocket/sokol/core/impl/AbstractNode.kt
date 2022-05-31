package com.github.aecsocket.sokol.core.impl

import com.github.aecsocket.sokol.core.*

abstract class AbstractNode<N : AbstractNode<N>>(
    override var parent: NodeKey<N>? = null,
    override val children: MutableMap<String, N> = HashMap(),
) : Node.Mutable<N> {
    override fun get(key: String) = children[key]
    override fun get(path: Iterable<String>): N? {
        var cur = self
        path.forEach { cur = cur[it] ?: return null }
        return cur
    }
    override fun get(vararg path: String) = get(path.asIterable())
    override fun has(key: String) = children.containsKey(key)

    override fun detach() {
        parent = null
    }

    override fun attach(node: N, key: String) {
        parent = NodeKey(node, key)
    }

    override fun remove(key: String) {
        children.remove(key)
    }

    override fun set(key: String, value: N) {
        value.attach(self, key)
        children[key]?.detach()
        children[key] = value
    }

    override fun path(): NodePath = parent?.let { it.node.path() + it.key } ?: NodePath.EMPTY
    override fun root(): N = parent?.node?.root() ?: self

    override fun asRoot(): N = copy().apply {
        detach()
    }

    private fun walk(path: NodePath, action: (NodePath, N) -> Unit) {
        action(path, self)
        children.forEach { (key, child) ->
            child.walk(path + key, action)
        }
    }

    override fun walk(action: (NodePath, N) -> Unit) = walk(NodePath.EMPTY, action)
}
