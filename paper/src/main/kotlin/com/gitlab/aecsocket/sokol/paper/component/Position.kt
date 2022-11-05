package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.extension.location
import com.gitlab.aecsocket.sokol.core.*
import org.bukkit.Location
import org.bukkit.World
import kotlin.reflect.KClass

interface PositionRead : SokolComponent {
    override val componentType get() = PositionRead::class

    val world: World
    val transform: Transform
}

fun PositionRead.location() = transform.translation.location(world)

interface PositionWrite : SokolComponent {
    override val componentType get() = PositionWrite::class

    val world: World
    var transform: Transform
}

fun PositionWrite.location() = transform.translation.location(world)

object PositionTarget : SokolSystem

@Before(PositionTarget::class)
class PositionSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mAsChildTransform = mappers.componentMapper<AsChildTransform>()
    private val mPositionRead = mappers.componentMapper<PositionRead>()

    @Subscribe
    fun on(event: CompositeSystem.AttachTo, entity: SokolEntity) {
        val parentPosition = mPositionRead.getOr(event.parent) ?: return
        val asChildTransform = mAsChildTransform.getOr(entity)?.profile?.transform ?: Transform.Identity
        mPositionRead.set(entity, object : PositionRead {
            override val world get() = parentPosition.world
            override val transform: Transform
                get() = parentPosition.transform + asChildTransform
        })
    }
}
