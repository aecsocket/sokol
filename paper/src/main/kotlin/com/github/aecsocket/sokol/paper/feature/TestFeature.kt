package com.github.aecsocket.sokol.paper.feature

import com.github.aecsocket.alexandria.core.EventDispatcher
import com.github.aecsocket.alexandria.core.extension.force
import com.github.aecsocket.sokol.core.TreeState
import com.github.aecsocket.sokol.core.event.NodeEvent
import com.github.aecsocket.sokol.core.event.TestEvent
import com.github.aecsocket.sokol.paper.PaperDataNode
import com.github.aecsocket.sokol.paper.PaperFeature
import com.github.aecsocket.sokol.paper.SokolPlugin
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable

class TestFeature(
    private val plugin: SokolPlugin
) : PaperFeature {
    override val id: String
        get() = ID

    override fun deserialize(node: ConfigurationNode) = Profile(
        node.node("profile_field").getString("")
    )

    @ConfigSerializable
    inner class Profile(
        val profileField: String
    ) : PaperFeature.Profile {
        override fun createData() = Data(
            0
        )

        override fun deserialize(node: ConfigurationNode) = Data(
            node.node("data_field").getInt(0)
        )

        override fun deserialize(pdc: PersistentDataContainer) = Data(
            pdc.getOrDefault(plugin.key("data_field"), PersistentDataType.INTEGER, 0)
        )

        @ConfigSerializable
        inner class Data(
            val dataField: Int
        ) : PaperFeature.Data {
            override fun serialize(node: ConfigurationNode) {
                node.node("data_field").set(dataField)
            }

            override fun serialize(ctx: PersistentDataAdapterContext) = ctx.newPersistentDataContainer().apply {
                set(plugin.key("data_field"), PersistentDataType.INTEGER, dataField)
            }

            override fun createState() = State()

            override fun copy() = Data(dataField)

            inner class State : PaperFeature.State {
                override fun setUp(
                    events: EventDispatcher.Builder<NodeEvent<PaperDataNode, PaperFeature.State>>,
                    state: TreeState.NodeState<PaperFeature.State>
                ) {
                    events.addListener { event -> when (event) {
                        is TestEvent -> {
                            println("EVENT RECV - event data = ${event.data} | profile data = $profileField | state data = $dataField")
                        }
                    } }
                }
            }
        }
    }

    companion object {
        const val ID = "test"
    }
}
