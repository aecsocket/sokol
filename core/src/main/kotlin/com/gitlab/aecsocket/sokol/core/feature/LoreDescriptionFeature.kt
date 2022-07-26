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

object LoreDescriptionFeature : Keyed {
    override val id get() = "lore_description"

    abstract class Type<P : Feature.Profile<*>> : Feature<P> {
        override val id get() = LoreDescriptionFeature.id

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
    > : BaseFeature<S, D, C> {
        abstract override val type: Feature<*>
        abstract override val profile: Profile<*>

        override fun onEvent(event: NodeEvent, ctx: C) {
            if (!ctx.node.isRoot()) return
            if (event !is NodeEvent.OnHostUpdate) return
            val host = ctx.host as? ItemHost ?: return
            val locale = host.holder.host?.locale

            fun <R> i18n(action: I18N<Component>.() -> R) = i18n(locale, action)

            i18n { make("description.component.${ctx.node.component.id}") }?.let {
                host.addLore(it, i18n { make(tlKey("lore_header")) }?.join())
            }
        }
    }
}
