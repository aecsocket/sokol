package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

data class LocalTransformStatic(
    val profile: Profile
) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("local_transform_static")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = LocalTransformStatic::class
    override val key get() = Key

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()

    override fun write(node: ConfigurationNode) {}

    @ConfigSerializable
    data class Profile(
        @Setting(nodeFromParent = true) val transform: Transform,
    ) : NonReadingComponentProfile {
        override fun readEmpty() = LocalTransformStatic(this)
    }
}

@All(LocalTransformStatic::class)
@Before(LocalTransformTarget::class)
class LocalTransformStaticSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mLocalTransformStatic = mappers.componentMapper<LocalTransformStatic>()
    private val mLocalTransform = mappers.componentMapper<LocalTransform>()

    @Subscribe
    fun on(event: SokolEvent.Populate, entity: SokolEntity) {
        val staticLocalTransform = mLocalTransformStatic.get(entity)

        mLocalTransform.set(entity, LocalTransform(staticLocalTransform.profile.transform))
    }
}
