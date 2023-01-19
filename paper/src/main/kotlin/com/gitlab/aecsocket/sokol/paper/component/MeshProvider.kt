package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.Mesh
import com.gitlab.aecsocket.sokol.core.SokolComponent
import com.gitlab.aecsocket.sokol.core.SokolSystem
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.transientComponent
import org.bukkit.entity.Player

data class MeshEntry(
    val mesh: Mesh,
    val transform: Transform
)

data class MeshProvider(
    val create: (transform: Transform, trackedPlayers: () -> Iterable<Player>) -> List<MeshEntry>
) : SokolComponent {
    companion object {
        fun init(ctx: Sokol.InitContext) {
            ctx.transientComponent<MeshProvider>()
            ctx.system { MeshProviderTarget }
        }
    }

    override val componentType get() = MeshProvider::class
}

object MeshProviderTarget : SokolSystem
