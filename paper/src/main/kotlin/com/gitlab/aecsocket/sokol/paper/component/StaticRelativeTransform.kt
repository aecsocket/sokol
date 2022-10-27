package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

data class StaticRelativeTransform(
    val profile: Profile
) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("static_relative_transform")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = StaticRelativeTransform::class
    override val key get() = Key

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()

    override fun write(node: ConfigurationNode) {}

    @ConfigSerializable
    data class Profile(
        @Setting(nodeFromParent = true) val transform: Transform,
    ) : ComponentProfile {
        override fun read(tag: NBTTag) = StaticRelativeTransform(this)

        override fun read(node: ConfigurationNode) = StaticRelativeTransform(this)
    }
}

@All(StaticRelativeTransform::class)
class StaticRelativeTransformSystem(engine: SokolEngine) : SokolSystem {
    private val mStaticRelativeTransform = engine.componentMapper<StaticRelativeTransform>()

    @Subscribe
    fun on(event: SokolEvent.Populate, entity: SokolEntity) {
        val staticSlotTransform = mStaticRelativeTransform.map(entity)

        entity.components.set(RelativeTransform(staticSlotTransform.profile.transform))
    }
}
