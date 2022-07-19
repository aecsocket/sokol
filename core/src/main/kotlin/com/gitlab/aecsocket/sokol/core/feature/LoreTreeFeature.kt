package com.gitlab.aecsocket.sokol.core.feature

import com.gitlab.aecsocket.alexandria.core.keyed.Keyed
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
import java.util.*
import kotlin.collections.ArrayList

object LoreTreeFeature : Keyed {
    override val id get() = "lore_tree"

    abstract class Type<P : Feature.Profile<*>> : Feature<P> {
        override val id get() = LoreTreeFeature.id

        override val statTypes: Map<Key, Stat<*>> get() = emptyMap()
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
        S : Feature.State<S, D, C>, D : Feature.Data<S>, C : FeatureContext<*, *, *>,
    > : Feature.State<S, D, C> {
        abstract override val type: Feature<*>
        abstract override val profile: Profile<*>

        protected abstract fun i18n(locale: Locale?, action: I18N<Component>.() -> List<Component>): List<Component>

        protected abstract fun tableOf(
            rows: List<Pair<Int, Iterable<Component>>>,
            padding: Component,
            separator: Component,
        ): List<Component>

        override fun onEvent(event: NodeEvent, ctx: C) {
            if (!ctx.node.isRoot()) return
            if (event !is NodeEvent.OnHosted) return
            val host = ctx.host as? ItemHost ?: return

            fun i18n(action: I18N<Component>.() -> List<Component>) =
                i18n(host.locale, action)

            val rows = ArrayList<Pair<Int, List<Component>>>()
            val padding = i18n { safe(tlKey("padding")) }.join()
            val separator = i18n { safe(tlKey("column_separator")) }.join()

            fun DataNode.apply(depth: Int = 0) {
                component.slots.forEach { (key, slot) ->
                    val slotText = i18n { safe(tlKey("slot")) {
                        list("slot") { slot.localize(this).forEach { sub(it) } }
                    } }.join()

                    node(key)?.let { child ->
                        rows.add(depth to listOf(
                            slotText,
                            (i18n { safe(tlKey("component")) {
                                list("component") { child.component.localize(this).forEach { sub(it) } }
                            } }.join()
                        )))

                        child.apply(depth + 1)
                    } ?: rows.add(depth to listOf(
                        slotText,
                        i18n { safe(tlKey(if (slot.required) "required" else "empty")) }.join()
                    ))
                }
            }
            ctx.node.apply()

            host.addLore(tableOf(rows, padding, separator))
        }

        override fun serialize(tag: CompoundBinaryTag.Mutable) {}
    }
}
