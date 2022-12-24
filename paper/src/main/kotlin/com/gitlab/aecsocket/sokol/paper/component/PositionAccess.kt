package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.extension.location
import com.gitlab.aecsocket.sokol.core.*
import org.bukkit.World

data class PositionAccess(
    val world: World,
    var transform: Transform
) : SokolComponent {
    override val componentType get() = PositionAccess::class
}

fun PositionAccess.location() = transform.position.location(world)

object PositionAccessTarget : SokolSystem
