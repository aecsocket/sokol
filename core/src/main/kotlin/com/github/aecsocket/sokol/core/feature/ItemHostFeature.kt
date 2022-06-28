package com.github.aecsocket.sokol.core.feature

import com.github.aecsocket.sokol.core.Feature
import com.github.aecsocket.sokol.core.FeatureContext
import com.github.aecsocket.sokol.core.event.NodeEvent
import com.github.aecsocket.sokol.core.nbt.CompoundBinaryTag
import com.github.aecsocket.sokol.core.rule.Rule
import com.github.aecsocket.sokol.core.stat.Stat
import net.kyori.adventure.key.Key
import org.spongepowered.configurate.ConfigurationNode

abstract class ItemHostFeature<P : ItemHostFeature<P>.Profile<*>> : Feature<P> {
    override val id: String get() = ID

    override val statTypes: Map<Key, Stat<*>> get() = emptyMap() //todo
    override val ruleTypes: Map<Key, Class<Rule>> get() = emptyMap()

    abstract inner class Profile<D : Profile<D>.Data<*>> : Feature.Profile<D> {
        override val type: ItemHostFeature<P>
            get() = this@ItemHostFeature

        abstract inner class Data<S : State<S, *, *>> : Feature.Data<S> {
            override val type: ItemHostFeature<P>
                get() = this@ItemHostFeature

            override fun serialize(node: ConfigurationNode) {}

            override fun serialize(tag: CompoundBinaryTag.Mutable) {}
        }

        abstract inner class State<S : State<S, D, C>, D : Data<S>, C : FeatureContext<*, *, *>>: Feature.State<S, D, C> {
            override val type: ItemHostFeature<P>
                get() = this@ItemHostFeature

            override fun onEvent(event: NodeEvent, ctx: C) {}

            override fun serialize(tag: CompoundBinaryTag.Mutable) {}

            fun asItem() {} // todo
        }
    }

    companion object {
        const val ID = "item_representation"
    }
}
