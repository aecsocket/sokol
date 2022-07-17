package com.gitlab.aecsocket.sokol.core

import com.gitlab.aecsocket.alexandria.core.keyed.Keyed
import com.gitlab.aecsocket.glossa.core.Localizable
import net.kyori.adventure.text.Component

interface Blueprint<N : DataNode> : Keyed, Localizable<Component> {
    fun createNode(): N
}
