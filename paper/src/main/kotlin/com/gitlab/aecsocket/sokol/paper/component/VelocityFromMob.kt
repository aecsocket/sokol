package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.extension.TPS
import com.gitlab.aecsocket.alexandria.core.physics.Vector3
import com.gitlab.aecsocket.alexandria.paper.extension.alexandria
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.SokolAPI

object VelocityFromMob : SimplePersistentComponent {
    override val componentType get() = VelocityFromMob::class
    override val key = SokolAPI.key("velocity_from_mob")
    val Type = ComponentType.singletonComponent(key, this)
}

@All(VelocityFromMob::class, IsMob::class)
@None(VelocityRead::class)
@Before(VelocityReadTarget::class)
class VelocityFromMobSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mIsMob = ids.mapper<IsMob>()
    private val mVelocityRead = ids.mapper<VelocityRead>()

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        val mob = mIsMob.get(entity).mob

        mVelocityRead.set(entity, object : VelocityRead {
            override val linear get() = mob.velocity.alexandria() * TPS.toDouble()
            override val angular get() = Vector3.Zero
        })
    }
}
