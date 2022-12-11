package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

data class LocalTransformStatic(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("local_transform_static")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = LocalTransformStatic::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        @Setting(nodeFromParent = true) val transform: Transform
    ) : SimpleComponentProfile {
        override val componentType get() = LocalTransformStatic::class

        override fun createEmpty() = ComponentBlueprint { LocalTransformStatic(this) }
    }
}

@All(LocalTransformStatic::class)
@Before(LocalTransformTarget::class)
class LocalTransformStaticSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mLocalTransformStatic = ids.mapper<LocalTransformStatic>()
    private val mLocalTransform = ids.mapper<LocalTransform>()

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        val localTransformStatic = mLocalTransformStatic.get(entity).profile

        mLocalTransform.combine(entity, localTransformStatic.transform)
    }
}
