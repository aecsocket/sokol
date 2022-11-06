package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

data class AsChildTransform(val profile: Profile) : MarkerPersistentComponent {
    companion object {
        val Key = SokolAPI.key("as_child_transform")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = AsChildTransform::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        @Setting(nodeFromParent = true) val transform: Transform
    ) : NonReadingComponentProfile {
        override fun readEmpty() = AsChildTransform(this)
    }
}
