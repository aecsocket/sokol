package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import com.gitlab.aecsocket.sokol.paper.UpdateEvent
import com.gitlab.aecsocket.sokol.paper.persistentComponent

class PositionFromDelta : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("position_from_delta")
        val Type = ComponentType.singletonProfile(Key, Profile)

        fun init(ctx: Sokol.InitContext) {
            ctx.persistentComponent(Type)
            ctx.system { PositionFromDeltaSystem(it) }
            ctx.system { PositionFromDeltaForwardSystem(it) }
        }
    }

    override val componentType get() = PositionFromDelta::class
    override val key get() = Key

    var enabled = true

    object Profile : SimpleComponentProfile<PositionFromDelta> {
        override val componentType get() = PositionFromDelta::class

        override fun createEmpty() = ComponentBlueprint { PositionFromDelta() }
    }
}

@All(PositionFromDelta::class, IsChild::class, DeltaTransform::class)
class PositionFromDeltaSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mPositionFromDelta = ids.mapper<PositionFromDelta>()
    private val mIsChild = ids.mapper<IsChild>()
    private val mDeltaTransform = ids.mapper<DeltaTransform>()
    private val mPositionAccess = ids.mapper<PositionAccess>()

    object Update : SokolEvent

    @Subscribe
    fun on(event: Update, entity: SokolEntity) {
        val positionFromDelta = mPositionFromDelta.get(entity)
        if (!positionFromDelta.enabled) return

        val isChild = mIsChild.get(entity)
        val parent = isChild.parent
        val deltaTransform = mDeltaTransform.get(entity)

        val pPositionAccess = mPositionAccess.getOr(parent) ?: return
        val transform = pPositionAccess.transform * deltaTransform.transform
        if (mPositionAccess.has(entity)) mPositionAccess.get(entity).transform = transform
        else mPositionAccess.set(entity, PositionAccess(pPositionAccess.world, transform))
    }
}

@Before(PositionAccessTarget::class)
@After(DeltaTransformTarget::class)
class PositionFromDeltaForwardSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mComposite = ids.mapper<Composite>()

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        mComposite.forwardAll(entity, PositionFromDeltaSystem.Update)
    }

    @Subscribe
    fun on(event: UpdateEvent, entity: SokolEntity) {
        mComposite.forwardAll(entity, PositionFromDeltaSystem.Update)
    }
}
