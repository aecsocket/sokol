package com.gitlab.aecsocket.sokol.core

import com.gitlab.aecsocket.alexandria.core.physics.Quaternion
import com.gitlab.aecsocket.alexandria.core.physics.Transform
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import java.util.Locale

interface NodeHost {
    val locale: Locale?

    interface Static : NodeHost {
        val transform: Transform
    }

    interface Dynamic : Static {
        override var transform: Transform
    }

    interface Looking : Dynamic {
        var looking: Quaternion
    }
}

interface ItemHost : NodeHost {
    val holder: ItemHolder

    val key: Key
    var amount: Int
    var name: Component?

    fun addLore(lines: Iterable<Component>)
}

interface ItemHolder {
    val host: NodeHost?
}

interface EntityHost : NodeHost.Looking

interface BlockHost : NodeHost.Static
