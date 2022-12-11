package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.sokol.core.*

data class LocalTransform(
    var transform: Transform
) : SokolComponent {
    override val componentType get() = LocalTransform::class
}

fun ComponentMapper<LocalTransform>.addTo(entity: SokolEntity, transform: Transform) {
    getOr(entity)?.let {
        it.transform *= transform
    } ?: set(entity, LocalTransform(transform))
}

object LocalTransformTarget : SokolSystem
