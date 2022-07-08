package com.github.aecsocket.sokol.paper.feature

import com.github.aecsocket.alexandria.core.physics.SimpleBody
import com.github.aecsocket.alexandria.core.spatial.Transform
import com.github.aecsocket.alexandria.core.spatial.Vector3
import com.github.aecsocket.sokol.core.event.NodeEvent
import com.github.aecsocket.sokol.core.nbt.CompoundBinaryTag
import com.github.aecsocket.sokol.core.rule.Rule
import com.github.aecsocket.sokol.core.stat.Stat
import com.github.aecsocket.sokol.paper.InspectDrag
import com.github.aecsocket.sokol.paper.PaperFeature
import com.github.aecsocket.sokol.paper.PaperFeatureContext
import com.github.aecsocket.sokol.paper.SokolPlugin
import net.kyori.adventure.key.Key
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get

private const val ATTACHED_TRANSFORM = "attached_transform"
private const val BODIES = "bodies"
private const val SLOT_TRANSFORMS = "slot_transforms"
private const val DRAG = "drag"

class InspectFeature(
    private val plugin: SokolPlugin
) : PaperFeature {
    override val id: String get() = ID

    override val statTypes: Map<Key, Stat<*>> get() = emptyMap()
    override val ruleTypes: Map<Key, Class<Rule>> get() = emptyMap()

    private fun Vector3.tryNormalize() = if (sqrLength == 0.0) this else normalized

    override fun createProfile(node: ConfigurationNode) = Profile(
        node.node(ATTACHED_TRANSFORM).get { Transform.Identity },
        node.node(BODIES).get { ArrayList() },
        node.node(SLOT_TRANSFORMS).get { HashMap() },
        node.node(DRAG).get(),
    )

    inner class Profile(
        val attachedTransform: Transform,
        val bodies: List<SimpleBody>,
        val slotTransforms: Map<String, Transform>,
        val drag: InspectDrag?,
        val invAttachedTransform: Transform = attachedTransform.inverse,
    ) : PaperFeature.Profile {
        override val type get() = this@InspectFeature

        override fun createData() = Data()

        override fun createData(node: ConfigurationNode) = Data()
        override fun createData(tag: CompoundBinaryTag) = Data()

        inner class Data : PaperFeature.Data {
            override val type get() = this@InspectFeature
            val profile get() = this@Profile

            override fun serialize(node: ConfigurationNode) {}

            override fun serialize(tag: CompoundBinaryTag.Mutable) {}

            override fun createState() = State()

            override fun copy() = Data()
        }

        inner class State : PaperFeature.State {
            override val type get() = this@InspectFeature
            val profile get() = this@Profile

            override fun asData() = Data()

            override fun serialize(tag: CompoundBinaryTag.Mutable) {}

            override fun onEvent(
                event: NodeEvent,
                ctx: PaperFeatureContext
            ) {}
        }
    }

    companion object {
        const val ID = "inspect"
    }
}
