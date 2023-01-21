package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.transientComponent
import org.bukkit.entity.Player

data class PlayerTracked(
    val trackedPlayers: () -> Set<Player>
) : SokolComponent {
    companion object {
        fun init(ctx: Sokol.InitContext) {
            ctx.transientComponent<PlayerTracked>()
            ctx.system { PlayerTrackedTarget }
            ctx.system { PlayerTrackedUpdateTarget }
        }
    }

    override val componentType get() = PlayerTracked::class
}

object PlayerTrackedTarget : SokolSystem

object PlayerTrackedUpdateTarget : SokolSystem
