package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

data class ChildTransformStatic(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("child_transform_static")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = ChildTransformStatic::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        @Setting(nodeFromParent = true) val transform: Transform
    ) : SimpleComponentProfile {
        override val componentType get() = ChildTransformStatic::class

        override fun createEmpty() = ComponentBlueprint { ChildTransformStatic(this) }
    }
}

@All(ChildTransformStatic::class)
@Before(ChildTransformTarget::class)
class ChildTransformStaticSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mChildTransformStatic = ids.mapper<ChildTransformStatic>()
    private val mChildTransform = ids.mapper<ChildTransform>()

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        val childTransformStatic = mChildTransformStatic.get(entity).profile

        mChildTransform.getOr(entity)?.let { it.transform += childTransformStatic.transform }
            ?: mChildTransform.set(entity, ChildTransform(childTransformStatic.transform))
    }
}
