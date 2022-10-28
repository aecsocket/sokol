package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.sokol.core.SokolComponent
import com.gitlab.aecsocket.sokol.core.SokolSystem

data class LocalTransform(
    val transform: Transform
) : SokolComponent {
    override val componentType get() = LocalTransform::class
}

object LocalTransformTarget : SokolSystem
