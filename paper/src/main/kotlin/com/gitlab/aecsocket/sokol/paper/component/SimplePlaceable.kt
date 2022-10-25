package com.gitlab.aecsocket.sokol.paper.component

import com.gitlab.aecsocket.alexandria.core.keyed.Keyed
import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.glossa.core.force
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.PersistentComponent
import com.gitlab.aecsocket.sokol.paper.PersistentComponentFactory
import com.gitlab.aecsocket.sokol.paper.RegistryComponentType
import com.gitlab.aecsocket.sokol.paper.SokolAPI
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.NodeKey

private const val SIMPLE_PLACEABLES = "simple_placeables"

data class SimplePlaceable(val backing: Config) : PersistentComponent {
    companion object {
        val Key = SokolAPI.key("simple_placeable")
    }

    override val componentType get() = SimplePlaceable::class.java
    override val key get() = Key

    override fun write(ctx: NBTTagContext) = ctx.makeString(backing.id)

    override fun write(node: ConfigurationNode) {
        node.set(backing.id)
    }

    @ConfigSerializable
    data class Config(
        @NodeKey override val id: String,
        val placeTransform: Transform = Transform.Identity,
    ) : Keyed

    class Type : RegistryComponentType<Config>(Config::class, SimplePlaceable::class, SIMPLE_PLACEABLES) {
        override val key get() = Key

        override fun read(tag: NBTTag) = SimplePlaceable(entry(tag.asString()))

        override fun read(node: ConfigurationNode) = SimplePlaceable(entry(node.force()))

        override fun readFactory(node: ConfigurationNode): PersistentComponentFactory {
            val backing = entry(node.force())
            return PersistentComponentFactory { SimplePlaceable(backing) }
        }
    }
}

@All(SimplePlaceable::class, Placeable::class)
class SimplePlaceableInjectorSystem(engine: SokolEngine) : SokolSystem {
    private val mConsumer = engine.componentMapper<Placeable>()
    private val mProvider = engine.componentMapper<SimplePlaceable>()

    @Subscribe
    fun on(event: SokolEvent.Populate, entity: SokolEntityAccess) {
        val consumer = mConsumer.map(entity)
        val provider = mProvider.map(entity)

        consumer.placeTransform = provider.backing.placeTransform
    }
}
