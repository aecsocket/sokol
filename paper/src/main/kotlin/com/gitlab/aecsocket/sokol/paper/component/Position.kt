package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Quaternion
import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.core.physics.Vector3
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.glossa.core.force
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.extension.asQuaternion
import com.gitlab.aecsocket.sokol.core.extension.makeQuaternion
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import org.bukkit.World
import org.spongepowered.configurate.ConfigurationNode

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

data class Rotation(
    var rotation: Quaternion = Quaternion.Identity
) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("rotation")
        val Type = ComponentType.singletonProfile(Key, Profile)
    }

    override val componentType get() = Rotation::class
    override val key get() = Key

    override fun write(ctx: NBTTagContext) = ctx.makeQuaternion(rotation)

    override fun write(node: ConfigurationNode) {
        node.set(rotation)
    }

    object Profile : ComponentProfile {
        override fun read(tag: NBTTag) = Rotation(tag.asQuaternion())

        override fun read(node: ConfigurationNode) = Rotation(node.force())
    }
}

@All(PositionRead::class, Composite::class)
class PositionComposeSystem(engine: SokolEngine) : SokolSystem {
    private val mPositionRead = engine.componentMapper<PositionRead>()
    private val mComposite = engine.componentMapper<Composite>()
    private val mRelativeTransform = engine.componentMapper<RelativeTransform>()

    @Subscribe
    fun on(event: Compose, entity: SokolEntity) {
        val position = mPositionRead.map(entity)

        mComposite.forEachChild(entity) { (_, child) ->
            val relativeTransform = mRelativeTransform.mapOr(child)?.transform ?: Transform.Identity

            child.components.set(object : PositionRead {
                override val world get() = position.world

                override val transform: Transform
                    get() = position.transform + relativeTransform
            })

            child.call(Compose)
        }
    }

    @Subscribe
    fun on(event: SokolEvent.Populate, entity: SokolEntity) {
        entity.call(Compose)
    }

    object Compose : SokolEvent
}
