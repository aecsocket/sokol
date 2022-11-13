package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.sokol.core.SokolComponent
import com.gitlab.aecsocket.sokol.core.SokolSystem
import org.bukkit.entity.Player

interface PlayerTracked : SokolComponent {
    override val componentType get() = PlayerTracked::class

    fun trackedPlayers(): Set<Player>
}

object PlayerTrackedTarget : SokolSystem
