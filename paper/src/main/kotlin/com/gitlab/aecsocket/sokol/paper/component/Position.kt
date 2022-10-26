package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Quaternion
import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.glossa.core.force
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.extension.asQuaternion
import com.gitlab.aecsocket.sokol.core.extension.makeQuaternion
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import org.bukkit.World
import org.spongepowered.configurate.ConfigurationNode

interface Position : SokolComponent {
    override val componentType get() = Position::class

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
