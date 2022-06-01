package com.github.aecsocket.sokol.paper.feature

import com.github.aecsocket.alexandria.core.Input
import com.github.aecsocket.alexandria.core.Input.Companion.MOUSE_LEFT
import com.github.aecsocket.sokol.core.FeatureContext
import com.github.aecsocket.sokol.core.TreeState
import com.github.aecsocket.sokol.core.event.NodeEvent
import com.github.aecsocket.sokol.core.nbt.CompoundBinaryTag
import com.github.aecsocket.sokol.paper.*
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable

class TestFeature(
    private val plugin: SokolPlugin
) : PaperFeature {
    override val id: String
        get() = ID

    override fun createProfile(node: ConfigurationNode) = Profile(
        node.node("profile_field").getString("")
    )

    @ConfigSerializable
    inner class Profile(
        val profileField: String
    ) : PaperFeature.Profile {
        override fun createData() = Data(
            0
        )

        override fun serialize(node: ConfigurationNode) = Data(
            node.node("data_field").getInt(0)
        )

        override fun deserialize(tag: CompoundBinaryTag) = Data(
            tag.getInt("data_field") { 0 }
        )

        @ConfigSerializable
        inner class Data(
            val clicks: Int
        ) : PaperFeature.Data {
            override fun serialize(node: ConfigurationNode) {
                node.node("clicks").set(clicks)
            }

            override fun serialize(tag: CompoundBinaryTag.Mutable) {
                tag.setInt("clicks", clicks)
            }

            override fun createState() = State()

            override fun copy() = Data(clicks)

            inner class State : PaperFeature.State {
                override fun resolveDependencies(get: (String) -> PaperFeature.State?) {}

                override fun onEvent(
                    event: NodeEvent<PaperTreeState>,
                    ctx: FeatureContext<PaperDataNode, PaperNodeHost>
                ) {
                    when (event) {
                        is NodeEvent.OnInput -> {
                            when (val input = event.input) {
                                is Input.Mouse -> {
                                    if (input.button == MOUSE_LEFT) {
                                        when (val host = ctx.host) {
                                            is PaperNodeHost.OfStack -> {
                                                // todo uhhh?
                                                ctx.writeTag {
                                                    setInt("clicks", clicks + 1)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val ID = "test"
    }
}
