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

@All(IsChild::class, LocalTransform::class)
@None(PositionRead::class)
@Before(PositionTarget::class)
@After(PositionPreTarget::class)
class PositionSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mIsChild = ids.mapper<IsChild>()
    private val mLocalTransform = ids.mapper<LocalTransform>()
    private val mPositionRead = ids.mapper<PositionRead>()

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        val isChild = mIsChild.get(entity)
        val localTransform = mLocalTransform.get(entity)
        val pPositionRead = mPositionRead.getOr(isChild.parent) ?: return

        mPositionRead.set(entity, object : PositionRead {
            override val world get() = pPositionRead.world
            override val transform get() = pPositionRead.transform + localTransform.transform
        })
    }
}
