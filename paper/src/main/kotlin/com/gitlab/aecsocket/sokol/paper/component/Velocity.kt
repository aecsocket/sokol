package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Vector3
import com.gitlab.aecsocket.sokol.core.SokolComponent
import com.gitlab.aecsocket.sokol.core.SokolSystem

object VelocityTarget : SokolSystem

interface VelocityRead : SokolComponent {
    override val componentType get() = VelocityRead::class

    val linear: Vector3
}
