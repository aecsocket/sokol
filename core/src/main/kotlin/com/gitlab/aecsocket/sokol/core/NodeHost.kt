package com.gitlab.aecsocket.sokol.core

import com.gitlab.aecsocket.alexandria.core.physics.Quaternion
import com.gitlab.aecsocket.alexandria.core.physics.Transform
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import java.util.Locale

interface NodeHost<N : DataNode> {
    val locale: Locale?
    var node: N?

    interface Static<N : DataNode> : NodeHost<N> {
        val transform: Transform
    }

    interface Dynamic<N : DataNode> : Static<N> {
        override var transform: Transform
    }

    interface Looking<N : DataNode> : Dynamic<N> {
        var looking: Quaternion
    }
}

interface ItemHost<N : DataNode> : NodeHost<N> {
    val holder: ItemHolder<N>

    val key: Key
    var amount: Int
    var name: Component?

    fun addLore(lines: Iterable<Component>, header: Component? = null)
}

interface ItemHolder<N : DataNode> {
    val host: NodeHost<N>?
}

interface EntityHost<N : DataNode> : NodeHost.Looking<N>

interface BlockHost<N : DataNode> : NodeHost.Static<N>
