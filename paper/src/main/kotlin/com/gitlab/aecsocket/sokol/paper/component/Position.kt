package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.sokol.core.*
import org.bukkit.World

interface PositionRead : SokolComponent {
    override val componentType get() = PositionRead::class

    val world: World
    val transform: Transform
}

interface PositionWrite : SokolComponent {
    override val componentType get() = PositionWrite::class

    val world: World
    var transform: Transform
}

object PositionTarget : SokolSystem

@Before(PositionTarget::class)
class PositionSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mPosition = mappers.componentMapper<PositionRead>()
    private val mLocalTransform = mappers.componentMapper<LocalTransform>()

    @Subscribe
    fun on(event: CompositeSystem.AttachTo, entity: SokolEntity) {
        val parentPosition = mPosition.getOr(event.parent) ?: return
        val localTransform = mLocalTransform.getOr(entity)?.transform ?: Transform.Identity
        mPosition.set(entity, object : PositionRead {
            override val world get() = parentPosition.world
            override val transform: Transform
                get() = parentPosition.transform + localTransform
        })
    }
}
