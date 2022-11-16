package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.sokol.core.*

interface Removable : SokolComponent {
    override val componentType get() = Removable::class

    val removed: Boolean
}

object RemovablePreTarget : SokolSystem

object RemovableTarget : SokolSystem

@All(IsChild::class)
@None(Removable::class)
@Before(RemovableTarget::class)
@After(RemovablePreTarget::class)
class RemovableSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mIsChild = ids.mapper<IsChild>()
    private val mRemovable = ids.mapper<Removable>()

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        val isChild = mIsChild.get(entity)

        mRemovable.set(entity, mRemovable.getOr(isChild.parent) ?: return)
    }
}
