package com.gitlab.aecsocket.sokol.core.feature

import com.gitlab.aecsocket.alexandria.core.Quantifier
import com.gitlab.aecsocket.alexandria.core.keyed.Keyed
import com.gitlab.aecsocket.alexandria.core.sum
import com.gitlab.aecsocket.glossa.core.I18N
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.event.NodeEvent
import com.gitlab.aecsocket.sokol.core.nbt.CompoundBinaryTag
import com.gitlab.aecsocket.sokol.core.rule.Rule
import com.gitlab.aecsocket.sokol.core.stat.Stat
import net.kyori.adventure.extra.kotlin.join
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.spongepowered.configurate.ConfigurationNode

object NodeHolderFeature : Keyed {
    override val id get() = "node_holder"

    const val RULE = "rule"
    const val CAPACITY = "capacity"

    const val NODES = "nodes"
    const val NODE = "node"
    const val AMOUNT = "amount"

    abstract class Type<P : Feature.Profile<*>> : Feature<P> {
        override val id get() = NodeHolderFeature.id

        override val statTypes: Map<Key, Stat<*>> get() = emptyMap()
        override val ruleTypes: Map<Key, Class<Rule>> get() = emptyMap()
    }

    abstract class Profile<D : Feature.Data<*>>(
        val rule: Rule,
        val capacity: Int?,
    ) : Feature.Profile<D> {
        abstract override val type: Type<*>
    }

    abstract class Data<S : Feature.State<S, *, *>, N : DataNode>(
        val nodes: MutableList<Quantifier<N>>,
    ) : Feature.Data<S> {
        abstract override val type: Feature<*>
        abstract override val profile: Profile<*>

        override fun serialize(tag: CompoundBinaryTag.Mutable) {
            tag.newList(NODES).apply {
                nodes.forEach { (node, amount) ->
                    add(tag.newCompound().apply {
                        set(NODE, newCompound().apply { node.serialize(this) })
                        setInt(AMOUNT, amount)
                    })
                }
            }
        }

        override fun serialize(node: ConfigurationNode) {
            node.node(NODES).set(nodes)
        }
    }

    abstract class State<
        S : Feature.State<S, D, C>, D : Feature.Data<S>, C : FeatureContext<*, *, *>,
        N : DataNode,
    >(
        val nodes: MutableList<Quantifier<N>>,
    ) : BaseFeature<S, D, C>{
        abstract override val type: Feature<*>
        abstract override val profile: Profile<*>

        fun forceAdd(node: N, amount: Int) {
            if (nodes.any()) {
                val lastIdx = nodes.size - 1
                val (last, lastAmount) = nodes[lastIdx]
                if (node == last)
                    nodes[lastIdx] = Quantifier(last, lastAmount + amount)
                else
                    nodes.add(Quantifier(node, amount))
            } else nodes.add(Quantifier(node, amount))
        }

        fun add(node: N, amount: Int): Boolean {
            if (profile.rule.applies(node)) {
                forceAdd(node, amount)
                return true
            }
            return false
        }

        override fun onEvent(event: NodeEvent, ctx: C) {
            if (!ctx.node.isRoot()) return
            val host = ctx.host as? ItemHost ?: return
            val locale = host.holder.host?.locale

            fun <R> i18n(action: I18N<Component>.() -> R) = i18n(locale, action)

            when (event) {
                is NodeEvent.OnHostUpdate -> {
                    val lore = i18n { make(tlKey("lore.${profile.capacity?.let { "capacity" } ?: "no_capacity"}")) {
                        list("node") { nodes.forEach { qt -> map {
                            tl("name") { qt.obj.component }
                            raw("amount") { qt.amount }
                        } } }
                        raw("amount") { nodes.sum() }
                        raw("capacity") { profile.capacity ?: -1 }
                    } }

                    lore?.let {
                        host.addLore(lore, i18n { make(tlKey("lore_header"))?.join() })
                    }
                }
                is NodeEvent.OnClickAsCurrent -> {

                }
            }
        }
    }
}
