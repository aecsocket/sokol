package com.github.aecsocket.sokol.paper

import com.github.aecsocket.sokol.core.*
import com.github.aecsocket.sokol.core.event.NodeEvent
import com.github.aecsocket.sokol.core.impl.*
import com.github.aecsocket.sokol.core.nbt.BinaryTag
import com.github.aecsocket.sokol.core.nbt.CompoundBinaryTag
import com.github.aecsocket.sokol.core.rule.Rule
import com.github.aecsocket.sokol.core.stat.StatMap

typealias PaperNodeKey = NodeKey<PaperDataNode>

interface PaperFeature : Feature<PaperDataNode, PaperFeature.Profile> {
    interface Profile : Feature.Profile<Data> {
        override val type: PaperFeature
    }

    interface Data : Feature.Data<State> {
        override val type: PaperFeature

        fun copy(): Data
    }

    interface State : Feature.State<State, Data, PaperFeatureContext> {
        override val type: PaperFeature
    }
}

interface PaperFeatureContext : FeatureContext<PaperTreeState, PaperNodeHost, PaperDataNode>

class PaperComponent(
    id: String,
    features: Map<String, PaperFeature.Profile>,
    slots: Map<String, PaperSlot>,
    tags: Set<String>
) : AbstractComponent<PaperComponent, PaperFeature.Profile, PaperSlot>(id, features, slots, tags)

class PaperSlot(
    key: String,
    tags: Set<String>,
    val required: Boolean,
    val modifiable: Boolean,
    val compatible: Rule
) : SimpleSlot(key, tags)

class PaperBlueprint(
    id: String,
    node: PaperDataNode
) : AbstractBlueprint<PaperDataNode>(id, node)

class PaperDataNode(
    component: PaperComponent,
    features: MutableMap<String, PaperFeature.Data> = HashMap(),
    val legacyFeatures: MutableMap<String, BinaryTag> = HashMap(),
    parent: PaperNodeKey? = null,
    children: MutableMap<String, PaperDataNode> = HashMap(),
    val legacyChildren: MutableMap<String, BinaryTag> = HashMap()
) : AbstractDataNode<PaperDataNode, PaperComponent, PaperFeature.Data, PaperTreeState>(
    component, features, parent, children
) {
    override val self = this

    override fun serialize(tag: CompoundBinaryTag.Mutable) {
        tag.setString(ID, component.id)
        tag.newCompound(FEATURES).apply {
            legacyFeatures.forEach(::set)
            features.forEach { (key, feature) ->
                newCompound(key).apply {
                    feature.serialize(this)
                }
            }
        }
        tag.newCompound(CHILDREN).apply {
            legacyChildren.forEach(::set)
            children.forEach { (key, child) ->
                newCompound(key).apply {
                    child.serialize(this)
                }
            }
        }
    }

    override fun copy(): PaperDataNode = PaperDataNode(
        component,
        features.map { (key, value) -> key to value.copy() }.associate { it }.toMutableMap(),
        legacyFeatures,
        parent,
        children.map { (key, value) -> key to value.copy() }.associate { it }.toMutableMap(),
        legacyChildren
    )
}

class PaperTreeState(
    root: PaperDataNode,
    stats: StatMap,
    nodeStates: Map<PaperDataNode, Map<String, PaperFeature.State>>,
    val incomplete: List<NodePath>
) : AbstractTreeState<PaperTreeState, PaperDataNode, PaperNodeHost, PaperFeature.Data, PaperFeature.State>(
    root, stats, nodeStates
) {
    override val self: PaperTreeState
        get() = this

    override fun <E : NodeEvent> callEvent(host: PaperNodeHost, event: E): E {
        nodeStates.forEach { (node, states) ->
            val ctx = object : PaperFeatureContext {
                override val state: PaperTreeState
                    get() = this@PaperTreeState
                override val host: PaperNodeHost
                    get() = host
                override val node: DataNode
                    get() = node

                override fun writeNode(action: PaperDataNode.() -> Unit) {
                    TODO("Not yet implemented")
                }
            }
            states.forEach { (_, state) ->
                state.onEvent(event, ctx)
            }
        }
        return event
    }
}
