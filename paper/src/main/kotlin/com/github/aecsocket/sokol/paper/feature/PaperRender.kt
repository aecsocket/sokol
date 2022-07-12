package com.github.aecsocket.sokol.paper.feature

import com.github.aecsocket.alexandria.core.extension.force
import com.github.aecsocket.alexandria.core.physics.SimpleBody
import com.github.aecsocket.alexandria.core.physics.Transform
import com.github.aecsocket.sokol.core.feature.NodeRenderException
import com.github.aecsocket.sokol.core.feature.RenderData
import com.github.aecsocket.sokol.core.feature.RenderFeature
import com.github.aecsocket.sokol.core.feature.RenderSlot
import com.github.aecsocket.sokol.core.nbt.CompoundBinaryTag
import com.github.aecsocket.sokol.core.util.RenderMesh
import com.github.aecsocket.sokol.paper.*
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get

private const val BODIES = "bodies"
private const val MESHES = "meshes"
private const val SLOTS = "slots"

class PaperRender(
    private val plugin: Sokol
) : RenderFeature.Type<PaperFeature.Profile>(), PaperFeature {
    override fun createProfile(node: ConfigurationNode) = Profile(
        node.node(BODIES).get { HashSet() },
        node.node(MESHES).get { HashSet() },
        node.node(SLOTS).get { HashMap() },
        node.force(),
    )

    inner class Profile(
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

            override fun render(node: PaperDataNode, host: PaperNodeHost, transform: Transform) {
                if (host !is PaperNodeHost.Static)
                    throw NodeRenderException("Host must be of type ${PaperNodeHost.Static::class.qualifiedName}")
                plugin.renders.create(node, host.world, transform)
            }
        }
    }
}
