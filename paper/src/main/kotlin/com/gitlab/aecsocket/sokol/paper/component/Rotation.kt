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
    val dRotation: Delta<Quaternion>,
    val tag: NBTTag?
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
        rotation: Quaternion,
        tag: NBTTag? = null
    ) : this(Delta(rotation), tag)

    override fun write(ctx: NBTTagContext) = ctx.makeQuaternion(rotation)

    override fun writeDelta(tag: NBTTag): NBTTag {
        return dRotation.ifDirty { tag.makeQuaternion(it) } ?: tag
    }

    override fun serialize(node: ConfigurationNode) {
        node.set(rotation)
    }

    object Profile : ComponentProfile {
        override val componentType get() = Rotation::class

        override fun read(space: SokolSpaceAccess, tag: NBTTag) = Rotation(tag.asQuaternion(), tag)

        override fun deserialize(space: SokolSpaceAccess, node: ConfigurationNode) = Rotation(node.get { Quaternion.Identity })

        override fun createEmpty() = Rotation(Quaternion.Identity)
    }
}
