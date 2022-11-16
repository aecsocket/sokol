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
import org.spongepowered.configurate.ConfigurationNode

data class Rotation(
    val dRotation: Delta<Quaternion>
) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("rotation")
        val Type = ComponentType.singletonProfile(Key, Profile)
    }

    override val componentType get() = Rotation::class
    override val key get() = Key

    override val dirty get() = dRotation.dirty
    var rotation by dRotation

    constructor(
        rotation: Quaternion
    ) : this(Delta(rotation))

    override fun write(ctx: NBTTagContext) = ctx.makeQuaternion(rotation)

    override fun writeDelta(tag: NBTTag): NBTTag {
        return dRotation.ifDirty { tag.makeQuaternion(it) } ?: tag
    }

    override fun serialize(node: ConfigurationNode) {
        node.set(rotation)
    }

    object Profile : ComponentProfile {
        override val componentType get() = Rotation::class

        override fun read(tag: NBTTag): ComponentBlueprint<Rotation> {
            val rotation = tag.asQuaternion()

            return ComponentBlueprint { Rotation(rotation) }
        }

        override fun deserialize(node: ConfigurationNode): ComponentBlueprint<Rotation> {
            val rotation = node.force<Quaternion>()

            return ComponentBlueprint { Rotation(rotation) }
        }

        override fun createEmpty() = ComponentBlueprint { Rotation(Quaternion.Identity) }
    }
}

@All(Rotation::class)
@Before(LocalTransformTarget::class)
class RotationSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mRotation = ids.mapper<Rotation>()
    private val mLocalTransform = ids.mapper<LocalTransform>()

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        val rotation = mRotation.get(entity)

        mLocalTransform.addTo(entity, Transform(Vector3.Zero, rotation.rotation))
    }
}
