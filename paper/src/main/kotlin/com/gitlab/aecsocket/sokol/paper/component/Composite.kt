package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.sokol.core.*

object Composite {
    object Attach : SokolEvent
}

object IsRoot : SokolComponent {
    override val componentType get() = IsRoot::class
}

data class IsChild(
    val parent: SokolEntity,
    val root: SokolEntity
) : SokolComponent {
    override val componentType get() = IsChild::class
}

fun ComponentMapper<IsChild>.root(entity: SokolEntity) = getOr(entity)?.root ?: entity

fun ComponentMapper<IsChild>.firstParent(entity: SokolEntity, predicate: (SokolEntity) -> Boolean): SokolEntity? {
    var current = getOr(entity)?.parent ?: return null
    while (!predicate(current)) current = getOr(current)?.parent ?: return null
    return current
}

@None(IsChild::class)
class CompositeConstructSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mIsRoot = ids.mapper<IsRoot>()

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        mIsRoot.set(entity, IsRoot)
    }
}

@All(IsRoot::class, IsChild::class)
class CompositeRootSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mIsRoot = ids.mapper<IsRoot>()

    @Subscribe
    fun on(event: Composite.Attach, entity: SokolEntity) {
        mIsRoot.remove(entity)
    }
}
