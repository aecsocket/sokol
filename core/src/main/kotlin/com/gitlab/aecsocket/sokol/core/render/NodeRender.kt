package com.gitlab.aecsocket.sokol.core

import com.gitlab.aecsocket.alexandria.core.physics.Body
import com.gitlab.aecsocket.alexandria.core.physics.Transform

interface RenderMesh

interface RenderPart {
    val parent: RenderPart?
    val slots: Map<String, PartSlot<*>>
    val bodies: Collection<Body>
    val meshes: Collection<RenderMesh>

    val worldTransform: Transform

    fun root(): RenderPart
}

interface PartSlot<P : RenderPart> {
    val transform: Transform
    val bodies: Collection<Body>
    var child: P
}
