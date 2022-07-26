package com.gitlab.aecsocket.sokol.core.feature

import com.gitlab.aecsocket.alexandria.core.keyed.Keyed
import com.gitlab.aecsocket.glossa.core.I18N
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.event.NodeEvent
import com.gitlab.aecsocket.sokol.core.nbt.CompoundBinaryTag
import com.gitlab.aecsocket.sokol.core.rule.Rule
import com.gitlab.aecsocket.sokol.core.stat.ItemDescriptorStat
import com.gitlab.aecsocket.sokol.core.stat.Stat
import com.gitlab.aecsocket.sokol.core.stat.StringStat
import com.gitlab.aecsocket.sokol.core.stat.statTypes
import net.kyori.adventure.extra.kotlin.join
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.spongepowered.configurate.ConfigurationNode

object ItemHosterFeature : Keyed {
    override val id get() = "item_hoster"

    object Stats {
        val Item = ItemDescriptorStat(id, "item")
        val NameKey = StringStat(id, "name_key")

        val All = statTypes(Item, NameKey)
    }

    abstract class Type<P : Feature.Profile<*>> : Feature<P> {
        override val id get() = ItemHosterFeature.id

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

    abstract class State<
        S : Feature.State<S, D, C>, D : Feature.Data<S>, C : FeatureContext<T, *, *>,
        N : DataNode, H : ItemHolder<N>, T : TreeState.Scoped<T, *, in R>, R : ItemHost<N>
    > : BaseFeature<S, D, C>, ItemHoster<N, H, T, R> {
        abstract override val type: Feature<*>
        abstract override val profile: Profile<*>

        override fun onEvent(event: NodeEvent, ctx: C) {}

        protected abstract fun asHost(holder: H, state: T, descriptor: ItemDescriptor, action: (R) -> Unit): R

        protected abstract fun callEvents(state: T, host: R)

        override fun itemHosted(holder: H, state: T): R {
            fun <R> i18n(action: I18N<Component>.() -> R) =
                i18n(holder.host?.locale, action)

            val desc = state.stats.valueOr(Stats.Item) {
                throw HostCreationException("No value for stat with key '${Stats.Item.key}'")
            }

            return asHost(holder, state, desc) { host ->
                val name = (state.stats.value(Stats.NameKey)?.let {
                    i18n { make(it) }
                } ?: i18n { state.root.component.localize(this) }).join()
                host.name = name

                callEvents(state, host)
            }
        }
    }
}
