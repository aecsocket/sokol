package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.Sokol
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import com.gitlab.aecsocket.sokol.paper.persistentComponent
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

data class DeltaTransformStatic(val profile: Profile) : SimplePersistentComponent {
    companion object {
        val Key = SokolAPI.key("delta_transform_static")
        val Type = ComponentType.deserializing(Key, Profile::class)

        fun init(ctx: Sokol.InitContext) {
            ctx.persistentComponent(Type)
            ctx.system { DeltaTransformStaticSystem(it) }
            ctx.system { DeltaTransformStaticForwardSystem(it) }
        }
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
class DeltaTransformStaticSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mDeltaTransformStatic = ids.mapper<DeltaTransformStatic>()
    private val mDeltaTransform = ids.mapper<DeltaTransform>()

    object Construct : SokolEvent

    @Subscribe
    fun on(event: Construct, entity: SokolEntity) {
        val deltaTransformStatic = mDeltaTransformStatic.get(entity).profile

        mDeltaTransform.combine(entity, deltaTransformStatic.transform)
    }
}

@Before(DeltaTransformTarget::class)
class DeltaTransformStaticForwardSystem(ids: ComponentIdAccess) : SokolSystem {
    private val mComposite = ids.mapper<Composite>()

    @Subscribe
    fun on(event: ConstructEvent, entity: SokolEntity) {
        mComposite.forwardAll(entity, DeltaTransformStaticSystem.Construct)
    }
}
