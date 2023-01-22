package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import com.gitlab.aecsocket.sokol.paper.persistentComponent

object RemovableFromParent : SimplePersistentComponent {
    fun init(ctx: Sokol.InitContext) {
        ctx.persistentComponent(Type)
        ctx.system { RemovableFromParentSystem(it) }
        ctx.system { RemovableFromParentForwardSystem(it) }
    }

    override val componentType get() = RemovableFromParent::class
    override val key = SokolAPI.key("removable_from_parent")
    val Type = ComponentType.singletonComponent(key, this)
}

@All(RemovableFromParent::class, IsChild::class)
class RemovableFromParentSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mRemovable = ids.mapper<Removable>()
    private val mIsChild = ids.mapper<IsChild>()

    object Update : SokolEvent

    @Subscribe
    fun on(event: Update, entity: SokolEntity) {
        val parent = mIsChild.firstParent(entity) { mRemovable.has(it) } ?: return
        val pRemovable = mRemovable.get(parent)
        mRemovable.set(entity, pRemovable)
    }
}

@After(RemovableTarget::class) // parent needs to have the component set first
class RemovableFromParentForwardSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mComposite = ids.mapper<Composite>()

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        mComposite.forwardAll(entity, RemovableFromParentSystem.Update)
    }

    @Subscribe
    fun on(event: Composite.Attach, entity: SokolEntity) {
        mComposite.forwardAll(entity, RemovableFromParentSystem.Update)
    }
}
