package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.sokol.core.*

interface Removable : SokolComponent {
    override val componentType get() = Removable::class

    val removed: Boolean

    fun remove()
}
