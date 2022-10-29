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

@All(PositionRead::class, Composite::class)
class PositionSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mPosition = mappers.componentMapper<PositionRead>()
    private val mComposite = mappers.componentMapper<Composite>()
    private val mLocalTransform = mappers.componentMapper<LocalTransform>()

    @Subscribe
    fun on(event: Compose, entity: SokolEntity) {
        val position = mPosition.get(entity)

        mComposite.forEachChild(entity) { (_, child) ->
            mLocalTransform.getOr(child)?.let {
                val localTransform = it.transform

                mPosition.set(child, object : PositionRead {
                    override val world get() = position.world

                    override val transform: Transform
                        get() = position.transform + localTransform
                })

                child.call(Compose)
            }
        }
    }

    @Subscribe
    fun on(event: SokolEvent.Populate, entity: SokolEntity) {
        entity.call(Compose)
    }

    object Compose : SokolEvent
}
