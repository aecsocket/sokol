package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.sokol.core.SokolComponent
import com.jme3.bullet.collision.PhysicsRayTestResult
import org.bukkit.entity.Player

interface LookedAt : SokolComponent {
    override val componentType get() = LookedAt::class

    val player: Player
    val rayTestResult: PhysicsRayTestResult
}

fun lookedAt(player: Player, rayTestResult: PhysicsRayTestResult) = object : LookedAt {
    override val player get() = player
    override val rayTestResult get() = rayTestResult
}
