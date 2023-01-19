package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Shape
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import com.gitlab.aecsocket.sokol.paper.persistentComponent
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

data class HoverShape(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("hover_shape")
        val Type = ComponentType.deserializing(Key, Profile::class)

        fun init(ctx: Sokol.InitContext) {
            ctx.persistentComponent(Type)
        }
    }

    override val componentType get() = HoverShape::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        @Setting(nodeFromParent = true) val shape: Shape
    ) : SimpleComponentProfile<HoverShape> {
        override val componentType get() = HoverShape::class

        override fun createEmpty() = ComponentBlueprint { HoverShape(this) }
    }
}
