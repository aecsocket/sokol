package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

data class DeltaTransformStatic(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("delta_transform_static")
        val Type = ComponentType.deserializing(Key, Profile::class)
    }

    override val componentType get() = DeltaTransformStatic::class
    override val key get() = Key

    @ConfigSerializable
    data class Profile(
        @Setting(nodeFromParent = true) val transform: Transform
    ) : SimpleComponentProfile<DeltaTransformStatic> {
        override val componentType get() = DeltaTransformStatic::class

        override fun createEmpty() = ComponentBlueprint { DeltaTransformStatic(this) }
    }
}

@All(DeltaTransformStatic::class)
@Before(DeltaTransformTarget::class)
class DeltaTransformStaticSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mDeltaTransformStatic = ids.mapper<DeltaTransformStatic>()
    private val mDeltaTransform = ids.mapper<DeltaTransform>()

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        val deltaTransformStatic = mDeltaTransformStatic.get(entity).profile

        mDeltaTransform.combine(entity, deltaTransformStatic.transform)
    }
}
