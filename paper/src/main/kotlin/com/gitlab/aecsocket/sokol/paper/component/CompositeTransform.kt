package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.sokol.core.*

data class CompositeTransform(
    val transform: Transform
) : SokolComponent {
    override val componentType get() = CompositeTransform::class
}

@After(LocalTransformTarget::class)
@Before(CompositeSystem::class)
class CompositeTransformSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mLocalTransform = mappers.componentMapper<LocalTransform>()
    private val mCompositeTransform = mappers.componentMapper<CompositeTransform>()

    @Subscribe
    fun on(event: CompositeSystem.AttachTo, entity: SokolEntity) {
        val parentTransform = mCompositeTransform.getOr(event.parent)?.transform ?: return
        val compositeTransform = parentTransform + (mLocalTransform.getOr(entity)?.transform ?: Transform.Identity)
        mCompositeTransform.set(entity, CompositeTransform(compositeTransform))
    }

    @Subscribe
    fun on(event: SokolEvent.Populate, entity: SokolEntity) {
        val transform = mLocalTransform.getOr(entity)?.transform ?: Transform.Identity
        mCompositeTransform.set(entity, CompositeTransform(transform))
    }
}
