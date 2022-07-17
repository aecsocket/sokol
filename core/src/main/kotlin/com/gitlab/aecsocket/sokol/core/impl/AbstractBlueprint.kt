package com.gitlab.aecsocket.sokol.core.impl

import com.gitlab.aecsocket.glossa.core.I18N
import com.gitlab.aecsocket.sokol.core.*
import net.kyori.adventure.text.Component

abstract class AbstractBlueprint<N : DataNode.Scoped<N, *, *, *>>(
    override val id: String,
    private val node: N
) : com.gitlab.aecsocket.sokol.core.Blueprint<N> {
    override fun createNode() = node.copy()

    override fun localize(i18n: I18N<Component>) =
        i18n.safe("blueprint.$id")
}
