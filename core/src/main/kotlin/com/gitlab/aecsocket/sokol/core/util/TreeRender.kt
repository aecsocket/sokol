package com.gitlab.aecsocket.sokol.core.util

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.sokol.core.ItemDescriptor
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required

// TODO make the render system more generic

sealed interface RenderMesh {
    val transform: Transform

    @ConfigSerializable
    data class Static(
        override val transform: Transform = Transform.Identity,
        @Required val item: ItemDescriptor
    ) : RenderMesh

    @ConfigSerializable
    data class Dynamic(
        override val transform: Transform = Transform.Identity
    ) : RenderMesh
}
