package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.sokol.core.*

data class DeltaTransform(
    var transform: Transform
) : SokolComponent {
    override val componentType get() = DeltaTransform::class
}

fun ComponentMapper<DeltaTransform>.combine(entity: SokolEntity, transform: Transform) {
    getOr(entity)?.let {
        it.transform *= transform
    } ?: set(entity, DeltaTransform(transform))
}

object DeltaTransformTarget : SokolSystem
