package com.gitlab.aecsocket.sokol.core.feature

import com.gitlab.aecsocket.alexandria.core.ComponentTableRenderer
import com.gitlab.aecsocket.alexandria.core.TableRow
import com.gitlab.aecsocket.alexandria.core.extension.join
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

object LoreStatsFeature : Keyed {
    override val id get() = "lore_stats"

    abstract class Type<P : Feature.Profile<*>> : Feature<P> {
        override val id get() = LoreStatsFeature.id

        override val statTypes: Map<Key, Stat<*>> get() = emptyMap()
        override val ruleTypes: Map<Key, Class<Rule>> get() = emptyMap()
    }

    abstract class Profile<D : Feature.Data<*>>(
        val formats: List<List<StatRenderer<*>>>,
        val tableRenderer: ComponentTableRenderer,
    ) : Feature.Profile<D> {
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
    > : BaseFeature<S, D, C> {
        abstract override val type: Feature<*>
        abstract override val profile: Profile<*>

        protected abstract fun tableOf(
            rows: List<TableRow<Component>>,
            colSeparator: Component,
        ): List<Component>

        override fun onEvent(event: NodeEvent, ctx: C) {
            if (!ctx.node.isRoot()) return
            if (event !is NodeEvent.OnHostUpdate) return
            val host = ctx.host as? ItemHost ?: return
            val locale = host.holder.host?.locale

            fun <R> i18n(action: I18N<Component>.() -> R) = i18n(locale, action)

            /*
            val stats = ctx.state.stats

            val sections = ArrayList<List<TableRow<Component>>>()
            profile.formats.forEach { section ->
                val secRows = ArrayList<TableRow<Component>>()

                fun <T> StatRenderer<T>.apply() {

                }

                section.forEach { renderer -> renderer.apply() }

                sections.add(secRows)
            }

            val sectionSeparator = i18n { safe(tlKey("section_separator")) }
            val rows = sections.join(sectionSeparator.map { TableRow(it) })

            stats.entries.forEach { (key, node) ->

            }


            host.addLore(
                profile.tableRenderer.render(rows)
                tableOf(rows, i18n { safe(tlKey("column_separator")) }.join()),
                i18n { make(tlKey("lore_header")) }?.join())*/
        }
    }
}

interface StatRenderer<T> {
    val key: Key

    fun render(value: T): Component
}
