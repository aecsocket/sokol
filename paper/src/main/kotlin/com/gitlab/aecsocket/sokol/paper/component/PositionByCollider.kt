package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.craftbullet.core.transform
import com.gitlab.aecsocket.craftbullet.paper.CraftBulletAPI
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.extension.alexandria
import com.gitlab.aecsocket.sokol.core.extension.bullet
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import com.gitlab.aecsocket.sokol.paper.UpdateEvent

object PositionByCollider : SimplePersistentComponent {
    override val componentType get() = PositionByCollider::class
    override val key = SokolAPI.key("position_by_collider")
    val Type = ComponentType.singletonComponent(key, this)
}

@All(PositionByCollider::class, ColliderInstance::class, BasePosition::class)
@Before(PositionTarget::class)
@After(ColliderInstanceTarget::class, BasePositionTarget::class)
class PositionByColliderSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mColliderInstance = ids.mapper<ColliderInstance>()
    private val mBasePosition = ids.mapper<BasePosition>()
    private val mPositionRead = ids.mapper<PositionRead>()
    private val mPositionWrite = ids.mapper<PositionWrite>()

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        val (physObj) = mColliderInstance.get(entity)
        val basePosition = mBasePosition.get(entity)
        val body = physObj.body

        var transform = body.transform.alexandria()

        mPositionRead.set(entity, object : PositionRead {
            override val world get() = basePosition.world
            override val transform get() = transform
        })

        mPositionWrite.set(entity, object : PositionWrite {
            override val world get() = basePosition.world
            override var transform: Transform
                get() = transform
                set(value) {
                    transform = value
                    basePosition.transform = value
                    CraftBulletAPI.executePhysics {
                        body.transform = transform.bullet()
                    }
                }
        })
    }

    @Subscribe
    fun on(event: UpdateEvent, entity: SokolEntity) {
        val (physObj) = mColliderInstance.get(entity)
        val basePosition = mBasePosition.getOr(entity) ?: return
        val body = physObj.body

        basePosition.transform = body.transform.alexandria()
    }
}
