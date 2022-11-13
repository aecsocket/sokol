package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.Mesh
import com.gitlab.aecsocket.sokol.core.SokolComponent
import com.gitlab.aecsocket.sokol.core.SokolEvent
import com.gitlab.aecsocket.sokol.core.SokolSystem
import org.bukkit.entity.Player

data class MeshEntry(
    val mesh: Mesh,
    val transform: Transform
)

object Meshes : SokolComponent {
    override val componentType get() = Meshes::class

    data class Create(
        val meshes: MutableList<MeshEntry>,
        val transform: Transform,
        val getTrackedPlayers: () -> Iterable<Player>
    ) : SokolEvent
}

object MeshesTarget : SokolSystem
