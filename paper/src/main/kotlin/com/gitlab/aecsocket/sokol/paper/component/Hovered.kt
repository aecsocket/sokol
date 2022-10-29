package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.sokol.core.SokolComponent
import com.jme3.bullet.collision.PhysicsRayTestResult
import org.bukkit.entity.Player

interface Hovered : SokolComponent {
    override val componentType get() = Hovered::class

    val player: Player
    val rayTestResult: PhysicsRayTestResult
}

fun hovered(player: Player, rayTestResult: PhysicsRayTestResult) = object : Hovered {
    override val player get() = player
    override val rayTestResult get() = rayTestResult
}
