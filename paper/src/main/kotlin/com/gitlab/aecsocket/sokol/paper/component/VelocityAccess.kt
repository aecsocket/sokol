package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Vector3
import com.gitlab.aecsocket.sokol.core.SokolComponent
import com.gitlab.aecsocket.sokol.core.SokolSystem

interface VelocityAccess {
    val linear: Vector3
    val angular: Vector3
}

interface VelocityRead : VelocityAccess, SokolComponent {
    override val componentType get() = VelocityRead::class
}

object VelocityReadTarget : SokolSystem

