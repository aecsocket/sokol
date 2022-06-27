package com.github.aecsocket.sokol.core.feature

import com.github.aecsocket.sokol.core.Feature
import com.github.aecsocket.sokol.core.FeatureContext
import com.github.aecsocket.sokol.core.event.NodeEvent
import com.github.aecsocket.sokol.core.nbt.CompoundBinaryTag
import org.spongepowered.configurate.ConfigurationNode

abstract class ItemRepresentationFeature<P : ItemRepresentationFeature<P>.Profile<*>> : Feature<P> {
    override val id: String
        get() = ID

    abstract inner class Profile<D : Profile<D>.Data<*>> : Feature.Profile<D> {
        override val type: ItemRepresentationFeature<P>
            get() = this@ItemRepresentationFeature

        abstract inner class Data<S : State<S, *, *>> : Feature.Data<S> {
            override val type: ItemRepresentationFeature<P>
                get() = this@ItemRepresentationFeature

            override fun serialize(node: ConfigurationNode) {}

            override fun serialize(tag: CompoundBinaryTag.Mutable) {}
        }

        abstract inner class State<S : State<S, D, C>, D : Data<S>, C : FeatureContext<*, *, *>>: Feature.State<S, D, C> {
            override val type: ItemRepresentationFeature<P>
                get() = this@ItemRepresentationFeature

            override fun resolveDependencies(get: (String) -> S?) {}

            override fun onEvent(event: NodeEvent, ctx: C) {}

            override fun serialize(tag: CompoundBinaryTag.Mutable) {}

            fun asItem() {} // todo
        }
    }

    companion object {
        const val ID = "item_representation"
    }
}
