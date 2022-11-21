package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.extension.location
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.ReloadEvent
import org.bukkit.World
import kotlin.reflect.KClass

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

@All(IsChild::class, LocalTransform::class)
@None(PositionRead::class)
@Before(PositionTarget::class, RootLocalTransformTarget::class)
@After(PositionPreTarget::class)
class PositionSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mIsChild = ids.mapper<IsChild>()
    private val mLocalTransform = ids.mapper<LocalTransform>()
    private val mRootLocalTransform = ids.mapper<RootLocalTransform>()
    private val mPositionRead = ids.mapper<PositionRead>()

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        val isChild = mIsChild.get(entity)
        val localTransform = mLocalTransform.get(entity).transform
        val pRootLocalTransform = mRootLocalTransform.getOr(isChild.parent)?.transform ?: Transform.Identity
        val pPositionRead = mPositionRead.getOr(isChild.parent) ?: return

        mRootLocalTransform.set(entity, RootLocalTransform(pRootLocalTransform + localTransform))

        mPositionRead.set(entity, object : PositionRead {
            override val world get() = pPositionRead.world
            override val transform get() = pPositionRead.transform + localTransform
        })
    }
}
