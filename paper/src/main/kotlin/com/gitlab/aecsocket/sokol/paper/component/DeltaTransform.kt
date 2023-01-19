package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.transientComponent

data class DeltaTransform(
    var transform: Transform
) : SokolComponent {
    companion object {
        fun init(ctx: Sokol.InitContext) {
            ctx.transientComponent<DeltaTransform>()
            ctx.system { DeltaTransformTarget }
        }
    }

    override val componentType get() = DeltaTransform::class
}

fun ComponentMapper<DeltaTransform>.combine(entity: SokolEntity, transform: Transform) {
    getOr(entity)?.let {
        it.transform *= transform
    } ?: set(entity, DeltaTransform(transform))
}

object DeltaTransformTarget : SokolSystem
