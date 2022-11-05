package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.sokol.core.*

data class CompositeTransform(
    val transform: Transform
) : SokolComponent {
    override val componentType get() = CompositeTransform::class
}

@Before(CompositeSystem::class)
class CompositeTransformSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mAsChildTransform = mappers.componentMapper<AsChildTransform>()
    private val mCompositeTransform = mappers.componentMapper<CompositeTransform>()

    @Subscribe
    fun on(event: CompositeSystem.AttachTo, entity: SokolEntity) {
        val parentTransform = mCompositeTransform.getOr(event.parent)?.transform ?: return
        val asChildTransform = mAsChildTransform.getOr(entity)?.profile?.transform ?: Transform.Identity
        mCompositeTransform.set(entity, CompositeTransform(parentTransform + asChildTransform))
    }

    @Subscribe
    fun on(event: SokolEvent.Populate, entity: SokolEntity) {
        mCompositeTransform.set(entity, CompositeTransform(Transform.Identity))
    }
}
