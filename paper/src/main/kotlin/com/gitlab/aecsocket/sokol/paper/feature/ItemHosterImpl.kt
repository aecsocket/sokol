package com.gitlab.aecsocket.sokol.paper.feature

import com.gitlab.aecsocket.sokol.core.feature.ItemHoster
import com.gitlab.aecsocket.sokol.core.feature.ItemHosterFeature
import com.gitlab.aecsocket.sokol.core.nbt.CompoundBinaryTag
import com.gitlab.aecsocket.sokol.paper.*
import com.gitlab.aecsocket.sokol.paper.extension.asStack
import net.kyori.adventure.extra.kotlin.join
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.TextDecoration
import org.spongepowered.configurate.ConfigurationNode

interface PaperItemHoster : ItemHoster<PaperItemHolder, PaperTreeState, PaperItemHost>

class ItemHosterImpl(
    private val plugin: Sokol
) : ItemHosterFeature.Type<PaperFeature.Profile>(), PaperFeature {
    override fun createProfile(node: ConfigurationNode) = Profile()

    inner class Profile : ItemHosterFeature.Profile<PaperFeature.Data>(), PaperFeature.Profile {
        override val type: ItemHosterImpl get() = this@ItemHosterImpl

        override fun createData() = Data()

        override fun createData(node: ConfigurationNode) = Data()

        override fun createData(tag: CompoundBinaryTag) = Data()

        inner class Data : ItemHosterFeature.Data<PaperFeature.State>(), PaperFeature.Data {
            override val type: ItemHosterImpl get() = this@ItemHosterImpl
            override val profile: Profile get() = this@Profile

            override fun createState() = State()

            override fun copy() = Data()
        }

        inner class State : ItemHosterFeature.State<
            PaperFeature.State, PaperFeature.Data, PaperFeatureContext,
            PaperItemHolder, PaperTreeState, PaperItemHost
        >(), PaperFeature.State, PaperItemHoster {
            override val type: ItemHosterImpl get() = this@ItemHosterImpl
            override val profile: Profile get() = this@Profile

            override fun asData() = Data()

            override fun itemHosted(holder: PaperItemHolder, state: PaperTreeState): PaperItemHost {
                val stack = descriptor(state).asStack()
                return useHostOf(holder, { stack }, { host ->
                    host.writeMeta { meta ->
                        meta.displayName(text().decoration(TextDecoration.ITALIC, false)
                            .append(state.root.component.localize(
                                plugin.i18n.apply { withLocale(holder.host?.locale ?: locale) }
                            ).join())
                            .build()
                        )
                        plugin.persistence.nodeInto(
                            state.root,
                            plugin.persistence.forceNodeTagOf(meta.persistentDataContainer)
                        )
                    }
                    state.callEvent(host, PaperNodeEvent.OnHosted)
                })
            }
        }
    }
}
