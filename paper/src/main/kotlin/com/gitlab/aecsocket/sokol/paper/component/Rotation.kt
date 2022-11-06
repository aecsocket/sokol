package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Quaternion
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.extension.asQuaternion
import com.gitlab.aecsocket.sokol.core.extension.makeQuaternion
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get

data class Rotation(
    private val dRotation: Delta<Quaternion>
) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("rotation")
        val Type = ComponentType.singletonProfile(Key, Profile)
    }

    override val componentType get() = Rotation::class
    override val key get() = Key

    override val dirty get() = dRotation.dirty

    constructor(
        rotation: Quaternion = Quaternion.Identity
    ) : this(Delta(rotation))

    var rotation by dRotation

    override fun write(ctx: NBTTagContext) = ctx.makeQuaternion(rotation)

    override fun writeDelta(tag: NBTTag): NBTTag {
        return dRotation.ifDirty { tag.makeQuaternion(it) } ?: tag
    }

    override fun write(node: ConfigurationNode) {
        if (rotation != Quaternion.Identity) node.set(rotation)
    }

    object Profile : ComponentProfile {
        override fun read(tag: NBTTag) = Rotation(tag.asQuaternion())

        override fun read(node: ConfigurationNode) = Rotation(node.get { Quaternion.Identity })

        override fun readEmpty() = Rotation(Quaternion.Identity)
    }
}

@All(Rotation::class)
class RotationSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mRotation = mappers.componentMapper<Rotation>()

    private fun remove(entity: SokolEntity) {
        val rotation = mRotation.get(entity)

        rotation.rotation = Quaternion.Identity
    }

    @Subscribe
    fun on(event: SokolEvent.Reset, entity: SokolEntity) {
        remove(entity)
    }

    @Subscribe
    fun on(event: SokolEvent.Remove, entity: SokolEntity) {
        remove(entity)
    }
}
