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


object PositionTarget : SokolSystem

interface PositionRead : SokolComponent, PositionAccess {
    override val componentType get() = PositionRead::class
}

interface PositionWrite : SokolComponent, PositionAccess {
    override val componentType get() = PositionWrite::class

    override var transform: Transform
}

data class ChildTransform(var transform: Transform) : SokolComponent {
    override val componentType get() = ChildTransform::class
}

object ChildTransformTarget : SokolSystem

@All(IsChild::class)
@None(PositionRead::class)
@After(PositionTarget::class, ChildTransformTarget::class)
class PositionReadSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mIsChild = ids.mapper<IsChild>()
    private val mPositionRead = ids.mapper<PositionRead>()
    private val mChildTransform = ids.mapper<ChildTransform>()

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        val isChild = mIsChild.get(entity)
        val childTransform = mChildTransform.getOr(entity)?.transform ?: Transform.Identity
        val pPositionRead = mPositionRead.getOr(isChild.parent) ?: return

        mPositionRead.set(entity, object : PositionRead {
            override val world get() = pPositionRead.world
            override val transform get() = pPositionRead.transform + childTransform
        })
    }
}
