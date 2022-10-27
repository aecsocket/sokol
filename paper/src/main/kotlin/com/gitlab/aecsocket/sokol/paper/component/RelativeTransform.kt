package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.sokol.core.SokolComponent

data class RelativeTransform(
    val transform: Transform
) : SokolComponent {
    override val componentType get() = RelativeTransform::class
}
