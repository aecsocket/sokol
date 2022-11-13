package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.extension.location
import com.gitlab.aecsocket.sokol.core.SokolComponent
import com.gitlab.aecsocket.sokol.core.SokolSystem
import org.bukkit.World

interface PositionAccess {
    val world: World
    val transform: Transform
}

fun PositionAccess.location() = transform.translation.location(world)


object PositionTarget : SokolSystem

interface PositionRead : SokolComponent, PositionAccess {
    override val componentType get() = PositionRead::class
}

interface PositionWrite : SokolComponent, PositionAccess {
    override val componentType get() = PositionWrite::class

    override var transform: Transform
}
