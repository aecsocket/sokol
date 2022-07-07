package com.github.aecsocket.sokol.paper.feature

import com.github.aecsocket.alexandria.core.bound.Bound
import com.github.aecsocket.alexandria.core.extension.force
import com.github.aecsocket.alexandria.core.spatial.Vector3
import com.github.aecsocket.sokol.core.event.NodeEvent
import com.github.aecsocket.sokol.core.nbt.CompoundBinaryTag
import com.github.aecsocket.sokol.core.rule.Rule
import com.github.aecsocket.sokol.core.stat.Stat
import com.github.aecsocket.sokol.paper.*
import net.kyori.adventure.key.Key
import org.spongepowered.configurate.ConfigurationNode

private const val BOUND = "bound"
private const val SLOT_OFFSETS = "slot_offsets"

class InspectFeature(
    private val plugin: SokolPlugin
) : PaperFeature {
    override val id: String get() = ID

    override val statTypes: Map<Key, Stat<*>> get() = emptyMap()
    override val ruleTypes: Map<Key, Class<Rule>> get() = emptyMap()

    override fun createProfile(node: ConfigurationNode) = Profile(
        node.node(BOUND).force(),
        node.node(SLOT_OFFSETS).force(),
    )

    inner class Profile(
        val bound: Bound,
        val slotOffsets: Map<String, Vector3>
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
