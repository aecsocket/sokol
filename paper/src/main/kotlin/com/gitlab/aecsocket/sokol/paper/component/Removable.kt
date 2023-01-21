package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.transientComponent

interface Removable : SokolComponent {
    companion object {
        fun init(ctx: Sokol.InitContext) {
            ctx.transientComponent<Removable>()
            ctx.system { RemovableTarget }
        }
    }

    override val componentType get() = Removable::class

    val removed: Boolean

    fun remove(silent: Boolean = false)
}

object RemovableTarget : SokolSystem
