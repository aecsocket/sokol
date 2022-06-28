package com.github.aecsocket.sokol.core.feature

import com.github.aecsocket.sokol.core.Feature
import com.github.aecsocket.sokol.core.FeatureContext
import com.github.aecsocket.sokol.core.ItemDescriptor
import com.github.aecsocket.sokol.core.TreeState
import com.github.aecsocket.sokol.core.event.NodeEvent
import com.github.aecsocket.sokol.core.nbt.CompoundBinaryTag
import com.github.aecsocket.sokol.core.rule.Rule
import com.github.aecsocket.sokol.core.stat.ItemDescriptorStat
import com.github.aecsocket.sokol.core.stat.Stat
import com.github.aecsocket.sokol.core.stat.statTypes
import net.kyori.adventure.key.Key
import org.spongepowered.configurate.ConfigurationNode

abstract class ItemHostFeature<P : Feature.Profile<*>> : Feature<P> {
    override val id: String get() = ID

    object Stats {
        val ITEM = ItemDescriptorStat(ID, "item")

        val ALL = statTypes(ITEM)
    }

    override val statTypes: Map<Key, Stat<*>> get() = Stats.ALL
    override val ruleTypes: Map<Key, Class<Rule>> get() = emptyMap()

    abstract inner class Profile<D : Feature.Data<*>> : Feature.Profile<D> {
        override val type: ItemHostFeature<P> get() = this@ItemHostFeature

        abstract inner class Data<S : Feature.State<S, *, *>> : Feature.Data<S> {
            override val type: ItemHostFeature<P> get() = this@ItemHostFeature

            override fun serialize(node: ConfigurationNode) {}

            override fun serialize(tag: CompoundBinaryTag.Mutable) {}
        }

        abstract inner class State<S : State<S, D, C>, D : Data<S>, C : FeatureContext<*, *, *>>: Feature.State<S, D, C> {
            override val type: ItemHostFeature<P> get() = this@ItemHostFeature

            override fun onEvent(event: NodeEvent, ctx: C) {}

            override fun serialize(tag: CompoundBinaryTag.Mutable) {}

            fun asItem(state: TreeState): ItemDescriptor {
                return state.stats.nodeOr(Stats.ITEM).compute()
            }
        }
    }

    companion object {
        const val ID = "item_host"
    }
}
