package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Quaternion
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.glossa.core.force
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.extension.asQuaternion
import com.gitlab.aecsocket.sokol.core.extension.makeQuaternion
import com.gitlab.aecsocket.sokol.paper.MobEvent
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import com.gitlab.aecsocket.sokol.paper.persistentComponent
import org.spongepowered.configurate.ConfigurationNode

data class Rotation(
    val dRotation: Delta<Quaternion>
) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("rotation")
        val Type = ComponentType.singletonProfile(Key, Profile)

        fun init(ctx: Sokol.InitContext) {
            ctx.persistentComponent(Type)
            ctx.system { RotationSystem(it) }
        }
    }

    override val componentType get() = Rotation::class
    override val key get() = Key

    override val dirty get() = dRotation.dirty
    var rotation by dRotation

    constructor(
        rotation: Quaternion
    ) : this(Delta(rotation))

    override fun clean() {
        dRotation.clean()
    }

    override fun write(ctx: NBTTagContext) = ctx.makeQuaternion(rotation)

    override fun writeDelta(tag: NBTTag): NBTTag {
        return dRotation.ifDirty { tag.makeQuaternion(it) } ?: tag
    }

    override fun serialize(node: ConfigurationNode) {
        node.set(rotation)
    }

    object Profile : ComponentProfile<Rotation> {
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
class RotationSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mRotation = ids.mapper<Rotation>()

    @Subscribe
    fun on(event: MobEvent.RemoveFromWorld, entity: SokolEntity) {
        val rotation = mRotation.get(entity)
        rotation.rotation = Quaternion.Identity
    }
}
