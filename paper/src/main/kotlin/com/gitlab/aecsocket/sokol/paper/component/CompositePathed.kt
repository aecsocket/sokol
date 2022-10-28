package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.sokol.core.*

data class CompositePathed(
    val path: CompositePath
) : SokolComponent {
    override val componentType get() = CompositePathed::class
}

@All(Composite::class)
class CompositePathedSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mComposite = mappers.componentMapper<Composite>()
    private val mCompositePathed = mappers.componentMapper<CompositePathed>()

    @Subscribe
    fun on(event: Compose, entity: SokolEntity) {
        mComposite.forEachChild(entity) { (key, child) ->
            val newPath = event.currentPath + key
            mCompositePathed.set(child, CompositePathed(compositePathOf(newPath)))
            child.call(Compose(newPath))
        }
    }

    @Subscribe
    fun on(event: SokolEvent.Populate, entity: SokolEntity) {
        mCompositePathed.set(entity, CompositePathed(emptyCompositePath()))
        entity.call(Compose(emptyList()))
    }

    data class Compose(
        val currentPath: List<String>
    ) : SokolEvent
}
