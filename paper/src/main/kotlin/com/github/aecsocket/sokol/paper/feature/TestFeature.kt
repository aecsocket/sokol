package com.github.aecsocket.sokol.paper.feature

import com.github.aecsocket.alexandria.core.EventDispatcher
import com.github.aecsocket.alexandria.paper.extension.location
import com.github.aecsocket.sokol.core.TreeState
import com.github.aecsocket.sokol.core.event.NodeEvent
import com.github.aecsocket.sokol.paper.PaperFeature
import com.github.aecsocket.sokol.paper.PaperNodeEvent
import com.github.aecsocket.sokol.paper.PaperNodeHost
import com.github.aecsocket.sokol.paper.SokolPlugin
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import org.bukkit.Bukkit
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
                    events: EventDispatcher.Builder<PaperNodeEvent>,
                    node: TreeState.NodeState<PaperFeature.State>
                ) {
                    events.addListener { event -> when (event) {
                        is NodeEvent.Tick -> {
                            if (Bukkit.getCurrentTick() % 10 == 0) {
                                println("Ticked with host ${event.state.host}")
                                when (val host = event.state.host) {
                                    is PaperNodeHost.WithPosition -> {
                                        val world = host.world
                                        val (x, y, z) = host.position
                                        world.playSound(Sound.sound(
                                            Key.key("block.dispenser.dispense"), Sound.Source.BLOCK, 1f, 1f
                                        ), x, y, z)
                                    }
                                }
                            }
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
