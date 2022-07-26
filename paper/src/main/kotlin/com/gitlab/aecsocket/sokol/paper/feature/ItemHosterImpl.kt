package com.gitlab.aecsocket.sokol.paper.feature

import com.gitlab.aecsocket.sokol.core.ItemDescriptor
import com.gitlab.aecsocket.sokol.core.feature.ItemHoster
import com.gitlab.aecsocket.sokol.core.feature.ItemHosterFeature
import com.gitlab.aecsocket.sokol.core.nbt.CompoundBinaryTag
import com.gitlab.aecsocket.sokol.paper.*
import org.spongepowered.configurate.ConfigurationNode

interface PaperItemHoster : ItemHoster<PaperDataNode, PaperItemHolder, PaperTreeState, PaperItemHost>

class ItemHosterImpl(
    val plugin: Sokol
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
            PaperDataNode, PaperItemHolder, PaperTreeState, PaperItemHost
        >(), PaperFeature.State, PaperItemHoster {
            override val type: ItemHosterImpl get() = this@ItemHosterImpl
            override val profile: Profile get() = this@Profile
            override val plugin: Sokol get() = this@ItemHosterImpl.plugin

            override fun asData() = Data()

            override fun asHost(
                holder: PaperItemHolder,
                state: PaperTreeState,
                descriptor: ItemDescriptor,
                action: (PaperItemHost) -> Unit,
            ): PaperItemHost {
                val stack = descriptor.asStack()
                return plugin.useHostOf(holder, { stack },
                    action = { host ->
                        host.writeMeta { meta ->
                            plugin.persistence.holderOf(meta.persistentDataContainer)[plugin.persistence.kNode] =
                                plugin.persistence.newTag().apply { state.root.serialize(this) }
                        }
                        action(host)
                    }
                )
            }

            override fun callEvents(state: PaperTreeState, host: PaperItemHost) {
                state.callEvent(host, PaperNodeEvent.OnHosted)
                state.callEvent(host, PaperNodeEvent.OnHostUpdate)
            }
        }
    }
}
