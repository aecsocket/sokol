package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.with
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.SokolAPI

object HeldAttachable : SimplePersistentComponent {
    override val key = SokolAPI.key("held_attachable")
    override val componentType get() = HeldAttachable::class
    val Type = ComponentType.singletonComponent(key, this)
}

@All(HeldAttachable::class, Held::class, ColliderInstance::class)
@After(ColliderInstanceTarget::class, HoldMovableColliderSystem::class)
class HeldAttachableSystem(ids: ComponentIdAccess) : SokolSystem {
    companion object {
        val Attach = HeldAttachable.key.with("attach")
    }

    private val mHeldAttachable = ids.mapper<HeldAttachable>()
    private val mHeld = ids.mapper<Held>()
    private val mColliderInstance = ids.mapper<ColliderInstance>()


    @Subscribe
    fun on(event: ColliderSystem.PostPhysicsStep, entity: SokolEntity) {
        val (hold) = mHeld.get(entity)
        val (physObj) = mColliderInstance.get(entity)
        val player = hold.player

        val operation = hold.operation as? MoveHoldOperation ?: return
        if (hold.frozen) return
    }
}
