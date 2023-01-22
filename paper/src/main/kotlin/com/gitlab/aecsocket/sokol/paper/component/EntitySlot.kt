package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Shape
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.transientComponent

interface EntitySlot : SokolComponent {
    companion object {
        fun init(ctx: Sokol.InitContext) {
            ctx.transientComponent<EntitySlot>()
            ctx.system { EntitySlotTarget }
        }
    }

    override val componentType get() = EntitySlot::class

    val shape: Shape

    fun full(): Boolean

    fun allows(child: SokolEntity): Boolean

    fun attach(child: SokolEntity)
}

object EntitySlotTarget : SokolSystem
