package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import com.gitlab.aecsocket.sokol.paper.UpdateEvent
import com.gitlab.aecsocket.sokol.paper.persistentComponent

class PositionFromParent : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("position_from_parent")
        val Type = ComponentType.singletonProfile(Key, Profile)

        fun init(ctx: Sokol.InitContext) {
            ctx.persistentComponent(Type)
            ctx.system { PositionFromParentSystem(it) }
            ctx.system { PositionFromParentForwardSystem(it) }
        }
    }

    override val componentType get() = PositionFromParent::class
    override val key get() = Key

    var enabled = true

    object Profile : SimpleComponentProfile<PositionFromParent> {
        override val componentType get() = PositionFromParent::class

        override fun createEmpty() = ComponentBlueprint { PositionFromParent() }
    }
}

@All(PositionFromParent::class, IsChild::class)
class PositionFromParentSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mPositionFromParent = ids.mapper<PositionFromParent>()
    private val mIsChild = ids.mapper<IsChild>()
    private val mDeltaTransform = ids.mapper<DeltaTransform>()
    private val mPositionAccess = ids.mapper<PositionAccess>()

    object Update : SokolEvent

    @Subscribe
    fun on(event: Update, entity: SokolEntity) {
        val positionFromDelta = mPositionFromParent.get(entity)
        if (!positionFromDelta.enabled) return
        val deltaTransform = mDeltaTransform.getOr(entity)?.transform ?: Transform.Identity

        val parent = mIsChild.firstParent(entity) { mPositionAccess.has(it) } ?: return
        val pPositionAccess = mPositionAccess.get(parent)

        val transform = pPositionAccess.transform * deltaTransform
        if (mPositionAccess.has(entity)) mPositionAccess.get(entity).transform = transform
        else mPositionAccess.set(entity, PositionAccess(pPositionAccess.world, transform))
    }
}

@Before(PositionAccessTarget::class)
@After(DeltaTransformTarget::class)
class PositionFromParentForwardSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mComposite = ids.mapper<Composite>()

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        mComposite.forwardAll(entity, PositionFromParentSystem.Update)
    }

    @Subscribe
    fun on(event: UpdateEvent, entity: SokolEntity) {
        mComposite.forwardAll(entity, PositionFromParentSystem.Update)
    }
}
