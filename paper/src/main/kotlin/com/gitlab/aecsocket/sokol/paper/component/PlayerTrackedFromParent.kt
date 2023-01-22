package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import com.gitlab.aecsocket.sokol.paper.persistentComponent

object PlayerTrackedFromParent : SimplePersistentComponent {
    fun init(ctx: Sokol.InitContext) {
        ctx.persistentComponent(Type)
        ctx.system { PlayerTrackedFromParentSystem(it) }
        ctx.system { PlayerTrackedFromParentForwardSystem(it) }
    }

    override val componentType get() = PlayerTrackedFromParent::class
    override val key = SokolAPI.key("player_tracked_from_parent")
    val Type = ComponentType.singletonComponent(key, this)
}

@All(PlayerTrackedFromParent::class, IsChild::class)
class PlayerTrackedFromParentSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mPlayerTracked = ids.mapper<PlayerTracked>()
    private val mIsChild = ids.mapper<IsChild>()

    object Update : SokolEvent

    @Subscribe
    fun on(event: Update, entity: SokolEntity) {
        val parent = mIsChild.firstParent(entity) { mPlayerTracked.has(it) } ?: return
        val pParentTracked = mPlayerTracked.get(parent)
        mPlayerTracked.set(entity, pParentTracked)
    }
}

@Before(PlayerTrackedUpdateTarget::class)
@After(PlayerTrackedTarget::class) // parent needs to have the component set first
class PlayerTrackedFromParentForwardSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mComposite = ids.mapper<Composite>()

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        mComposite.forwardAll(entity, PlayerTrackedFromParentSystem.Update)
    }

    @Subscribe
    fun on(event: Composite.Attach, entity: SokolEntity) {
        mComposite.forwardAll(entity, PlayerTrackedFromParentSystem.Update)
    }
}
