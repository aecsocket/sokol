package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable

data class StaticPlaceable(val profile: Profile) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("static_placeable")
        val Type = ComponentType.deserializing<Profile>(Key)
    }

    override val componentType get() = StaticPlaceable::class
    override val key get() = Key

    override fun write(ctx: NBTTagContext) = ctx.makeCompound()

    override fun write(node: ConfigurationNode) {}

    @ConfigSerializable
    data class Profile(
        val placeTransform: Transform = Transform.Identity,
        val holdDistance: Double = 0.0,
        val snapDistance: Double = 0.0,
    ) : ComponentProfile {
        override fun read(tag: NBTTag) = StaticPlaceable(this)

        override fun read(node: ConfigurationNode) = StaticPlaceable(this)
    }
}

@All(StaticPlaceable::class)
class StaticPlaceableSystem(mappers: ComponentIdAccess) : SokolSystem {
    private val mStaticPlaceable = mappers.componentMapper<StaticPlaceable>()
    private val mPlaceable = mappers.componentMapper<Placeable>()

    @Subscribe
    fun on(event: SokolEvent.Populate, entity: SokolEntity) {
        val staticPlaceable = mStaticPlaceable.get(entity).profile

        mPlaceable.set(entity, Placeable(
            staticPlaceable.placeTransform,
            staticPlaceable.holdDistance,
            staticPlaceable.snapDistance
        ))
    }
}
