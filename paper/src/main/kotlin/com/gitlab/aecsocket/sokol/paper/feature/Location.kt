package com.gitlab.aecsocket.sokol.paper.feature

import com.gitlab.aecsocket.alexandria.core.physics.Quaternion
import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.glossa.core.force
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.extension.asQuaternion
import com.gitlab.aecsocket.sokol.core.extension.ofQuaternion
import com.gitlab.aecsocket.sokol.paper.NBTWriter
import com.gitlab.aecsocket.sokol.paper.PersistentComponent
import com.gitlab.aecsocket.sokol.paper.PersistentComponentType
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import net.kyori.adventure.key.Key
import org.bukkit.World
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get

interface Location : SokolComponent {
    override val componentType get() = Location::class.java

    val world: World
    var transform: Transform
}

private const val ROTATION = "rotation"

data class Rotation(
    var rotation: Quaternion
) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("rotation")
    }

    override val componentType get() = Rotation::class.java
    override val key get() = Key

    override fun write(): NBTWriter = { ofQuaternion(rotation) }

    override fun write(node: ConfigurationNode) {
        node.node(ROTATION).set(rotation)
    }

    object Type : PersistentComponentType {
        override val key get() = Key

        override fun read(tag: NBTTag) = Rotation(tag.asQuaternion())

        override fun read(node: ConfigurationNode) = Rotation(
            node.node(ROTATION).get { Quaternion.Identity }
        )
    }
}
