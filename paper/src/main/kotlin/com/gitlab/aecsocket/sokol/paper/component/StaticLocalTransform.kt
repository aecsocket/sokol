package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

data class StaticLocalTransform(
    val profile: Profile
) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("static_local_transform")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = StaticLocalTransform::class
    override val key get() = Key

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()

    override fun write(node: ConfigurationNode) {}

    @ConfigSerializable
    data class Profile(
        @Setting(nodeFromParent = true) val transform: Transform,
    ) : ComponentProfile {
        override fun read(tag: NBTTag) = StaticLocalTransform(this)

        override fun read(node: ConfigurationNode) = StaticLocalTransform(this)
    }
}

@All(StaticLocalTransform::class)
@Before(PositionSystem::class, CompositeTransformSystem::class)
class StaticLocalTransformSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mStaticLocalTransform = mappers.componentMapper<StaticLocalTransform>()
    private val mLocalTransform = mappers.componentMapper<LocalTransform>()

    @Subscribe
    fun on(event: SokolEvent.Populate, entity: SokolEntity) {
        val staticLocalTransform = mStaticLocalTransform.get(entity)

        mLocalTransform.set(entity, LocalTransform(staticLocalTransform.profile.transform))
    }
}