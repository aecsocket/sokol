package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.extension.location
import com.gitlab.aecsocket.sokol.core.*
import org.bukkit.World

interface PositionAccess {
    val world: World
    val transform: Transform
}

fun PositionAccess.location() = transform.translation.location(world)

interface PositionRead : SokolComponent, PositionAccess {
    override val componentType get() = PositionRead::class
}

interface PositionWrite : SokolComponent, PositionAccess {
    override val componentType get() = PositionWrite::class

    override var transform: Transform
}

object PositionPreTarget : SokolSystem

object PositionTarget : SokolSystem

data class RootLocalTransform(val transform: Transform) : SokolComponent {
    override val componentType get() = RootLocalTransform::class
}

object RootLocalTransformTarget : SokolSystem

@All(IsChild::class)
@Before(PositionTarget::class, RootLocalTransformTarget::class)
@After(PositionPreTarget::class)
class PositionSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mIsChild = ids.mapper<IsChild>()
    private val mLocalTransform = ids.mapper<LocalTransform>()
    private val mRootLocalTransform = ids.mapper<RootLocalTransform>()
    private val mPositionRead = ids.mapper<PositionRead>()

    private fun construct(entity: SokolEntity) {
        val isChild = mIsChild.get(entity)
        val localTransform = mLocalTransform.getOr(entity)?.transform ?: Transform.Identity
        val pRootLocalTransform = mRootLocalTransform.getOr(isChild.parent)?.transform ?: Transform.Identity
        val pPositionRead = mPositionRead.getOr(isChild.parent) ?: return

        mRootLocalTransform.set(entity, RootLocalTransform(pRootLocalTransform * localTransform))

        mPositionRead.set(entity, object : PositionRead {
            override val world get() = pPositionRead.world
            override val transform get() = pPositionRead.transform * localTransform
        })
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
