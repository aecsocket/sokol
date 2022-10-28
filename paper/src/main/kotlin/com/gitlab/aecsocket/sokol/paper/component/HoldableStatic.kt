package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.EntityHolding
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

data class HoldableStatic(val profile: Profile) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("holdable_static")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = HoldableStatic::class
    override val key get() = Key

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()

    override fun write(node: ConfigurationNode) {}

    @ConfigSerializable
    data class Profile(
        @Setting(nodeFromParent = true) val settings: EntityHolding.StateSettings
    ) : NonReadingComponentProfile {
        override fun readEmpty() = HoldableStatic(this)
    }
}

@All(HoldableStatic::class)
@Before(HoldableTarget::class)
class HoldableStaticSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mHoldableStatic = mappers.componentMapper<HoldableStatic>()
    private val mHoldable = mappers.componentMapper<Holdable>()

    @Subscribe
    fun on(event: SokolEvent.Populate, entity: SokolEntity) {
        val holdableStatic = mHoldableStatic.get(entity)

        mHoldable.set(entity, Holdable(holdableStatic.profile.settings))
    }
}
