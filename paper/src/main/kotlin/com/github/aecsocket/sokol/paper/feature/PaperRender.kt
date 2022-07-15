package com.github.aecsocket.sokol.paper.feature

import com.github.aecsocket.alexandria.core.extension.force
import com.github.aecsocket.alexandria.core.physics.SimpleBody
import com.github.aecsocket.alexandria.core.physics.Transform
import com.github.aecsocket.alexandria.paper.effect.playGlobal
import com.github.aecsocket.alexandria.paper.extension.scheduleDelayed
import com.github.aecsocket.alexandria.paper.input.InputMapper
import com.github.aecsocket.sokol.core.event.NodeEvent
import com.github.aecsocket.sokol.core.feature.NodeRenderException
import com.github.aecsocket.sokol.core.feature.RenderData
import com.github.aecsocket.sokol.core.feature.RenderFeature
import com.github.aecsocket.sokol.core.feature.RenderSlot
import com.github.aecsocket.sokol.core.nbt.CompoundBinaryTag
import com.github.aecsocket.sokol.core.util.RenderMesh
import com.github.aecsocket.sokol.paper.*
import org.bukkit.GameMode
import org.bukkit.inventory.EquipmentSlot
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get

private const val INPUTS = "inputs"
private const val BODIES = "bodies"
private const val MESHES = "meshes"
private const val SLOTS = "slots"

private const val PLACE = "place"

class PaperRender(
    private val plugin: Sokol
) : RenderFeature.Type<PaperFeature.Profile>(), PaperFeature {
    override fun createProfile(node: ConfigurationNode) = Profile(
        node.node(INPUTS).get { InputMapper.Empty },
        node.node(BODIES).get { HashSet() },
        node.node(MESHES).get { HashSet() },
        node.node(SLOTS).get { HashMap() },
        node.force(),
    )

    inner class Profile(
        val inputs: InputMapper,
        bodies: Collection<SimpleBody>,
        meshes: Collection<RenderMesh>,
        slots: Map<String, RenderSlot>,
        data: RenderData,
    ) : RenderFeature.Profile<PaperFeature.Data>(
        bodies, meshes, slots, data,
    ), PaperFeature.Profile {
        override val type: PaperRender get() = this@PaperRender

        override fun createData() = Data()

        override fun createData(node: ConfigurationNode) = Data()

        override fun createData(tag: CompoundBinaryTag) = Data()

        inner class Data : RenderFeature.Data<PaperFeature.State>(), PaperFeature.Data {
            override val type: PaperRender get() = this@PaperRender
            override val profile: Profile get() = this@Profile

            override fun createState() = State()

            override fun copy() = Data()
        }

        inner class State : RenderFeature.State<PaperFeature.State, PaperFeature.Data, PaperFeatureContext, PaperNodeHost, PaperDataNode>(), PaperFeature.State {
            override val type: PaperRender get() = this@PaperRender
            override val profile: Profile get() = this@Profile

            override fun asData() = Data()

            override fun onEvent(event: NodeEvent, ctx: PaperFeatureContext) {
                if (!ctx.node.isRoot()) return
                val host = ctx.host as? PaperNodeHost.OfStack ?: return
                val holder = host.holder as? StackHolder.ByEquipment ?: return
                if (holder.slot != EquipmentSlot.HAND) return

                when (event) {
                    is PaperNodeEvent.OnInput -> {
                        val (input, player) = event
                        inputs.actionOf(input, player)?.let {
                            when (it) {
                                PLACE -> {
                                    plugin.scheduleDelayed {
                                        if (player.gameMode != GameMode.CREATIVE) {
                                            host.stack.subtract()
                                        }
                                        val transform = plugin.renders.computePartTransform(
                                            plugin.playerState(player).renders,
                                            data.surfaceOffset)
                                        data.soundPlace.playGlobal(plugin.effectors, player.world, transform.tl)
                                        render(ctx.node, holder.parent, transform)
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }

            override fun render(node: PaperDataNode, host: PaperNodeHost, transform: Transform) {
                if (host !is PaperNodeHost.Static)
                    throw NodeRenderException("Host must be of type ${PaperNodeHost.Static::class.qualifiedName}")
                plugin.renders.create(node, host.world, transform)
            }
        }
    }
}
