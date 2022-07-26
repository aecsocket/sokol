package com.gitlab.aecsocket.sokol.paper.feature

import com.gitlab.aecsocket.alexandria.core.Quantifier
import com.gitlab.aecsocket.sokol.core.feature.NodeHolderFeature
import com.gitlab.aecsocket.sokol.core.feature.NodeHolderFeature.AMOUNT
import com.gitlab.aecsocket.sokol.core.feature.NodeHolderFeature.CAPACITY
import com.gitlab.aecsocket.sokol.core.feature.NodeHolderFeature.NODE
import com.gitlab.aecsocket.sokol.core.feature.NodeHolderFeature.NODES
import com.gitlab.aecsocket.sokol.core.feature.NodeHolderFeature.RULE
import com.gitlab.aecsocket.sokol.core.nbt.CompoundBinaryTag
import com.gitlab.aecsocket.sokol.core.nbt.TagSerializationException
import com.gitlab.aecsocket.sokol.core.rule.Rule
import com.gitlab.aecsocket.sokol.paper.PaperDataNode
import com.gitlab.aecsocket.sokol.paper.PaperFeature
import com.gitlab.aecsocket.sokol.paper.PaperFeatureContext
import com.gitlab.aecsocket.sokol.paper.Sokol
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get

class NodeHolderImpl(
    val plugin: Sokol
) : NodeHolderFeature.Type<PaperFeature.Profile>(), PaperFeature {
    override fun createProfile(node: ConfigurationNode) = Profile(
        node.node(RULE).get { Rule.True },
        node.node(CAPACITY).get { 0 },
    )

    inner class Profile(
        rule: Rule,
        capacity: Int?
    ) : NodeHolderFeature.Profile<PaperFeature.Data>(
        rule, capacity,
    ), PaperFeature.Profile {
        override val type: NodeHolderImpl get() = this@NodeHolderImpl

        override fun createData() = Data(ArrayList())

        override fun createData(node: ConfigurationNode) = Data(
            node.node(NODES).get { ArrayList() }
        )

        override fun createData(tag: CompoundBinaryTag) = Data(
            tag.getList(NODES)?.mapIndexed { idx, qtTag ->
                val qt = qtTag.forceCompound()
                Quantifier(
                    plugin.persistence.nodeOf(qt.forceCompound(NODE))
                        // we throw here rather than just discard the node
                        // since otherwise it would lead to data loss
                        // safer to just outright refuse to make this feature work
                        ?: throw TagSerializationException("Could not deserialize node at index $idx"),
                    qt.forceInt(AMOUNT)
                )
            }?.toMutableList() ?: ArrayList()
        )

        inner class Data(
            nodes: MutableList<Quantifier<PaperDataNode>>,
        ) : NodeHolderFeature.Data<PaperFeature.State, PaperDataNode>(nodes), PaperFeature.Data {
            override val type: NodeHolderImpl get() = this@NodeHolderImpl
            override val profile: Profile get() = this@Profile

            override fun createState() = State(nodes)

            override fun copy() = Data(nodes.map { it.copy() }.toMutableList())
        }

        inner class State(
            nodes: MutableList<Quantifier<PaperDataNode>>,
        ) : NodeHolderFeature.State<
            PaperFeature.State, PaperFeature.Data, PaperFeatureContext,
            PaperDataNode,
        >(nodes), PaperFeature.State {
            override val type: NodeHolderImpl get() = this@NodeHolderImpl
            override val profile: Profile get() = this@Profile
            override val plugin: Sokol get() = this@NodeHolderImpl.plugin

            override fun asData() = Data(nodes)
        }
    }
}