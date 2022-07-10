package com.github.aecsocket.sokol.core.util

import com.github.aecsocket.alexandria.core.physics.Body
import com.github.aecsocket.alexandria.core.physics.Transform
import com.github.aecsocket.sokol.core.DataNode
import com.github.aecsocket.sokol.core.ItemDescriptor
import com.github.aecsocket.sokol.core.feature.RenderFeature
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required

// TODO make the render system generic

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

/*
interface TreeRender<N : DataNode> {
    val root: TreeRenderPart<N>

    var transform: Transform
}

class TreeRenderPart<N : DataNode>(
    val node: N,
    var bodies: Collection<Body>,
    var meshes: Collection<TreeRenderMesh>,
    var slots: Map<String, TreeRenderSlot<N>>,
) {
    fun setFrom(feature: RenderFeature.Profile<*>) {
        //bodies = feature.bodies.map {  }
    }
}

interface TreeRenderMesh {
    fun transform(tf: Transform)
}

data class TreeRenderSlot<N : DataNode>(
    val transform: Transform,
    var part: TreeRenderPart<N>? = null,
)*/

/*
class TreeRenderPart<N : DataNode>(
    val node: N,
    val bodies: Collection<DynamicBody>,
    val meshes: Collection<TreeRenderMesh>,
    val slots: Map<String, TreeRenderSlot<N>>,
    val asChildTransform: Transform,
    transform: Transform = Transform.Identity,
) {
    val invAsChildTransform = asChildTransform.inverse

    /*var transform: Transform = transform
        set(value) {
            slots.forEach { (_, slot) ->
                slot.part?.let { it.transform =  }
            }
            field = value
        }*/

    fun walk(action: (TreeRenderPart<N>) -> Unit) {
        action(this)
        slots.forEach { (_, slot) -> slot.part?.walk(action) }
    }
}
*/