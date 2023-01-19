package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Vector3
import com.gitlab.aecsocket.sokol.core.SokolComponent
import com.gitlab.aecsocket.sokol.core.SokolSystem
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.transientComponent

interface VelocityAccess : SokolComponent {
    companion object {
        fun init(ctx: Sokol.InitContext) {
            ctx.transientComponent<VelocityAccess>()
            ctx.system { VelocityAccessTarget }
        }
    }

    override val componentType get() = VelocityAccess::class

    val linear: Vector3
    val angular: Vector3
}

object VelocityAccessTarget : SokolSystem
