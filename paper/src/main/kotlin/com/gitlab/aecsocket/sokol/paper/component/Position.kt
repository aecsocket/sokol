package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.extension.location
import com.gitlab.aecsocket.sokol.core.*
import org.bukkit.World
import kotlin.reflect.KClass

interface WorldAccess : SokolComponent {
    override val componentType get() = WorldAccess::class

    val world: World
}

object WorldAccessPreTarget : SokolSystem

object WorldAccessTarget : SokolSystem

interface PositionAccess {
    val world: World
    val transform: Transform
}

fun PositionAccess.location() = transform.translation.location(world)

interface BasePosition : SokolComponent, PositionAccess {
    override val componentType get() = PositionRead::class

    override var transform: Transform
}

object BasePositionTarget : SokolSystem

interface PositionRead : SokolComponent, PositionAccess {
    override val componentType get() = PositionRead::class
}

interface PositionWrite : SokolComponent, PositionAccess {
    override val componentType get() = PositionWrite::class

    override var transform: Transform
}

object PositionPreTarget : SokolSystem

object PositionTarget : SokolSystem

@All(IsChild::class)
@None(WorldAccess::class)
@Before(WorldAccessTarget::class)
@After(WorldAccessPreTarget::class)
class WorldAccessSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mIsChild = ids.mapper<IsChild>()
    private val mWorldAccess = ids.mapper<WorldAccess>()

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        val isChild = mIsChild.get(entity)

        mWorldAccess.set(entity, mWorldAccess.getOr(isChild.parent) ?: return)
    }
}

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
