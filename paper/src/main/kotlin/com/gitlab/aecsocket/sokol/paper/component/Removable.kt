package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.transientComponent

interface Removable : SokolComponent {
    companion object {
        fun init(ctx: Sokol.InitContext) {
            ctx.transientComponent<Removable>()
            ctx.system { RemovablePreTarget }
            ctx.system { RemovableTarget }
            ctx.system { RemovableSystem(it) }
        }
    }

    override val componentType get() = Removable::class

    val removed: Boolean

    fun remove(silent: Boolean = false)
}

object RemovablePreTarget : SokolSystem

object RemovableTarget : SokolSystem

@All(IsChild::class)
@Before(RemovableTarget::class)
@After(RemovablePreTarget::class)
class RemovableSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mIsChild = ids.mapper<IsChild>()
    private val mRemovable = ids.mapper<Removable>()

    private fun construct(entity: SokolEntity) {
        val isChild = mIsChild.get(entity)

        mRemovable.set(entity, mRemovable.getOr(isChild.parent) ?: return)
    }

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        construct(entity)
    }

    @Subscribe
    fun on(event: Composite.Attach, entity: SokolEntity) {
        construct(entity)
    }
}
