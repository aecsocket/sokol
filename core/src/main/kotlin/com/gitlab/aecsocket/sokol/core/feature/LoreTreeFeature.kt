package com.gitlab.aecsocket.sokol.core.feature

import com.gitlab.aecsocket.alexandria.core.ComponentTableRenderer
import com.gitlab.aecsocket.alexandria.core.TableRow
import com.gitlab.aecsocket.alexandria.core.extension.repeat
import com.gitlab.aecsocket.alexandria.core.keyed.Keyed
import com.gitlab.aecsocket.alexandria.core.tableDimensionsOf
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

object LoreTreeFeature : Keyed {
    override val id get() = "lore_tree"

    abstract class Type<P : Feature.Profile<*>> : Feature<P> {
        override val id get() = LoreTreeFeature.id

        override val statTypes: Map<Key, Stat<*>> get() = emptyMap()
        override val ruleTypes: Map<Key, Class<Rule>> get() = emptyMap()
    }

    abstract class Profile<D : Feature.Data<*>>(
        val atTop: List<String>,
        val atBottom: List<String>,
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

        protected abstract fun widthOf(value: Component): Int

        override fun onEvent(event: NodeEvent, ctx: C) {
            if (!ctx.node.isRoot()) return
            if (event !is NodeEvent.OnHostUpdate) return
            val host = ctx.host as? ItemHost ?: return
            val locale = host.holder.host?.locale

            fun <R> i18n(action: I18N<Component>.() -> R) = i18n(locale, action)

            data class SlotRow(val depth: Int, val row: TableRow<Component>)

            fun DataNode.apply(depth: Int = 0): List<SlotRow> {
                fun Slot.apply(key: String): List<SlotRow> {
                    val slotText = i18n { safe(tlKey("slot")) {
                        list("slot") { localize(this).forEach { sub(it) } }
                    } }

                    return node(key)?.let { child ->
                        listOf(SlotRow(depth, listOf(
                            slotText,
                            i18n { safe(tlKey("component")) {
                                list("component") { child.component.localize(this).forEach { sub(it) } }
                            } }
                        ))) + child.apply(depth + 1)
                    } ?: listOf(SlotRow(depth, listOf(
                        slotText,
                        i18n { safe(tlKey(if (required) "required" else "empty")) }
                    )))
                }

                val slotsLeft = component.slots.toMutableMap()

                // do the slots in `atTop` first
                val top = profile.atTop.flatMap { key ->
                    slotsLeft.remove(key)?.apply(key) ?: emptyList()
                }

                // then the slots in `atBottom`
                val bottom = profile.atBottom.flatMap  { key ->
                    slotsLeft.remove(key)?.apply(key) ?: emptyList()
                }

                // then the unordered slots
                val remaining = slotsLeft.flatMap { (key, slot) -> slot.apply(key) }

                return top + remaining + bottom
            }

            val rows = ctx.node.apply()
            val dimensions = tableDimensionsOf(rows.map { it.row }, this::widthOf)

            val padding = i18n { safe(tlKey("padding")) }.join()
            host.addLore(
                rows.flatMap { (depth, row) ->
                    val rowPadding = padding.repeat(depth)
                    profile.tableRenderer.render(listOf(row), dimensions)
                        .map { rowPadding.append(it) }
                },
                i18n { make(tlKey("lore_header")) }?.join()
            )
        }
    }
}
