package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.sokol.core.*

data class CompositeTransform(
    val transform: Transform
) : SokolComponent {
    override val componentType get() = CompositeTransform::class
}

@All(LocalTransform::class)
@After(LocalTransformTarget::class)
class CompositeTransformSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mLocalTransform = mappers.componentMapper<LocalTransform>()
    private val mCompositeTransform = mappers.componentMapper<CompositeTransform>()
    private val mComposite = mappers.componentMapper<Composite>()

    @Subscribe
    fun on(event: Compose, entity: SokolEntity) {
        val localTransform = mLocalTransform.get(entity)

        val transform = event.transform + localTransform.transform
        mCompositeTransform.set(entity, CompositeTransform(transform))

        mComposite.forward(entity, Compose(transform))
    }

    @Subscribe
    fun on(event: SokolEvent.Populate, entity: SokolEntity) {
        entity.call(Compose(Transform.Identity))
    }

    data class Compose(
        val transform: Transform
    ) : SokolEvent
}
