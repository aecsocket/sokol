package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.Mesh
import com.gitlab.aecsocket.sokol.core.SokolComponent
import com.gitlab.aecsocket.sokol.core.SokolSystem
import org.bukkit.entity.Player

data class MeshProvider(
    val create: (transform: Transform, trackedPlayers: () -> Iterable<Player>) -> List<MeshEntry>
) : SokolComponent {
    override val componentType get() = MeshProvider::class
}

object MeshProviderTarget : SokolSystem

data class MeshEntry(
    val mesh: Mesh,
    val transform: Transform
)
