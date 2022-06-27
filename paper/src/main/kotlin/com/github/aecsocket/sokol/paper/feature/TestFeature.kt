package com.github.aecsocket.sokol.paper.feature

import com.github.aecsocket.alexandria.core.Input
import com.github.aecsocket.sokol.core.event.NodeEvent
import com.github.aecsocket.sokol.core.nbt.CompoundBinaryTag
import com.github.aecsocket.sokol.core.stat.DecimalStat
import com.github.aecsocket.sokol.paper.*
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable

class TestFeature(
    private val plugin: SokolPlugin
) : PaperFeature {
    object Stat {
        val SOME_INT = DecimalStat(ID, "some_int")
    }

    override val id: String
        get() = ID

    override fun createProfile(node: ConfigurationNode) = Profile(
        node.node("profile_field").getString("")
    )

    @ConfigSerializable
    inner class Profile(
        val profileField: String
    ) : PaperFeature.Profile {
        override val type: PaperFeature
            get() = this@TestFeature

        override fun createData() = Data(0, 0)

        override fun createData(node: ConfigurationNode) = Data(
            node.node("clicks").getInt(0),
            node.node("ticks_lived").getLong(0)
        )

        override fun createData(tag: CompoundBinaryTag) = Data(
            tag.getInt("clicks") { 0 },
            tag.getLong("ticks_lived") { 0 }
        )

        @ConfigSerializable
        inner class Data(
            val clicks: Int,
            val ticksLived: Long
        ) : PaperFeature.Data {
            override val type: PaperFeature
                get() = this@TestFeature

            override fun serialize(node: ConfigurationNode) {
                node.node("clicks").set(clicks)
            }

            override fun serialize(tag: CompoundBinaryTag.Mutable) {
                tag.setInt("clicks", clicks)
                tag.setLong("ticks_lived", ticksLived)
            }

            override fun createState() = State(clicks, ticksLived)

            override fun copy() = Data(clicks, ticksLived)
        }

        inner class State(
            var clicks: Int,
            var ticksLived: Long
        ) : PaperFeature.State {
            override val type: PaperFeature
                get() = this@TestFeature

            override fun asData() = Data(clicks, ticksLived)

            override fun serialize(tag: CompoundBinaryTag.Mutable) {
                tag.setInt("clicks", clicks)
                tag.setLong("ticks_lived", ticksLived)
            }

            override fun resolveDependencies(get: (String) -> PaperFeature.State?) {}

            override fun onEvent(
                event: NodeEvent,
                ctx: PaperFeatureContext
            ) {
                when (event) {
                    is NodeEvent.OnInput -> {
                        when (val input = event.input) {
                            is Input.Mouse -> {
                                if (input.button == Input.MouseButton.RIGHT) {
                                    when (val host = ctx.host) {
                                        is PaperNodeHost.OfStack -> {
                                            clicks++
                                            host.writeMeta {
                                                displayName(Component.text("Clicks: $clicks | Ticks lived: $ticksLived"))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    is NodeEvent.OnTick -> {
                        ticksLived++
                        if (Bukkit.getCurrentTick() % 10 == 0) {
                            when (val host = ctx.host) {
                                is PaperNodeHost.WithPosition -> host
                                is PaperNodeHost.OfStack -> {
                                    val root = host.holder.root()
                                    host.writeMeta {
                                        displayName(Component.text("Clicks: $clicks | Ticks lived: $ticksLived"))
                                    }
                                    if (root is PaperNodeHost.WithPosition) root else null
                                }
                                else -> null
                            }?.let { host ->
                                val (x, y, z) = host.position
                                host.world.playSound(Sound.sound(
                                    Key.key("block.dispenser.fail"), Sound.Source.BLOCK, 1f, 1f
                                ), x, y, z)
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
