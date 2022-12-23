package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.extension.location
import com.gitlab.aecsocket.sokol.core.*
import org.bukkit.World

interface PositionAccess {
    val world: World
    val transform: Transform
}

fun PositionAccess.location() = transform.position.location(world)

object PositionAccessTarget : SokolSystem

fun PositionAccess.asRead() = object : PositionRead {
    override val world get() = this@asRead.world
    override val transform get() = this@asRead.transform
}

interface PositionRead : PositionAccess, SokolComponent {
    override val componentType get() = PositionRead::class
}

data class PositionWrite(
    override val world: World,
    override var transform: Transform
) : PositionAccess, SokolComponent {
    override val componentType get() = PositionWrite::class
}
