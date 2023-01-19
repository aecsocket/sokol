package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.extension.location
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.transientComponent
import org.bukkit.World

data class PositionAccess(
    val world: World,
    var transform: Transform
) : SokolComponent {
    companion object {
        fun init(ctx: Sokol.InitContext) {
            ctx.transientComponent<PositionAccess>()
            ctx.system { PositionAccessTarget }
        }
    }

    override val componentType get() = PositionAccess::class
}

fun PositionAccess.location() = transform.position.location(world)

object PositionAccessTarget : SokolSystem
