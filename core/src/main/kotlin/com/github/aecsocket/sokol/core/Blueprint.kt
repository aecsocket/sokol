package com.github.aecsocket.sokol.core

import com.github.aecsocket.alexandria.core.keyed.Keyed
import com.github.aecsocket.glossa.core.Localizable
import net.kyori.adventure.text.Component

interface Blueprint<N : DataNode> : Keyed, Localizable<Component> {
    fun createNode(): N
}
