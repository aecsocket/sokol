package com.gitlab.aecsocket.sokol.core.feature

import com.gitlab.aecsocket.alexandria.core.keyed.Keyed
import com.gitlab.aecsocket.sokol.core.Feature
import com.gitlab.aecsocket.sokol.core.FeatureContext
import com.gitlab.aecsocket.sokol.core.ItemDescriptor
import com.gitlab.aecsocket.sokol.core.TreeState
import com.gitlab.aecsocket.sokol.core.event.NodeEvent
import com.gitlab.aecsocket.sokol.core.nbt.CompoundBinaryTag
import com.gitlab.aecsocket.sokol.core.rule.Rule
import com.gitlab.aecsocket.sokol.core.stat.ItemDescriptorStat
import com.gitlab.aecsocket.sokol.core.stat.Stat
import com.gitlab.aecsocket.sokol.core.stat.statTypes
import net.kyori.adventure.key.Key
import org.spongepowered.configurate.ConfigurationNode

object ItemHostFeature : Keyed {
    override val id get() = "item_host"

    object Stats {
        val Item = ItemDescriptorStat(id, "item")

        val All = statTypes(Item)
    }

    abstract class Type<P : Feature.Profile<*>> : Feature<P> {
        override val id get() = ItemHostFeature.id

        override val statTypes: Map<Key, Stat<*>> get() = Stats.All
        override val ruleTypes: Map<Key, Class<Rule>> get() = emptyMap()
    }

    abstract class Profile<D : Feature.Data<*>> : Feature.Profile<D> {
        abstract override val type: Type<*>
    }

    abstract class Data<S : Feature.State<S, *, *>> : Feature.Data<S> {
        abstract override val type: Feature<*>
        abstract override val profile: Profile<*>

        override fun serialize(tag: CompoundBinaryTag.Mutable) {}

        override fun serialize(node: ConfigurationNode) {}
    }

    abstract class State<S : Feature.State<S, D, C>, D : Feature.Data<S>, C : FeatureContext<*, *, *>> : Feature.State<S, D, C> {
        abstract override val type: Feature<*>
        abstract override val profile: Profile<*>

        override fun onEvent(event: NodeEvent, ctx: C) {}

        override fun serialize(tag: CompoundBinaryTag.Mutable) {}

        fun itemDescriptor(state: TreeState): ItemDescriptor {
            return state.stats.nodeOr(Stats.Item).compute()
        }
    }
}