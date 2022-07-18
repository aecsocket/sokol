package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.core.event.NodeEvent
import com.gitlab.aecsocket.sokol.core.impl.*
import com.gitlab.aecsocket.sokol.core.nbt.BinaryTag
import com.gitlab.aecsocket.sokol.core.nbt.CompoundBinaryTag
import com.gitlab.aecsocket.sokol.core.rule.Rule
import com.gitlab.aecsocket.sokol.core.stat.ApplicableStats
import com.gitlab.aecsocket.sokol.core.stat.CompiledStatMap
import org.checkerframework.checker.units.qual.N
import org.spongepowered.configurate.ConfigurationNode

typealias PaperNodeKey = NodeKey<PaperDataNode>

interface PaperFeature : Feature<PaperFeature.Profile> {
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
    tags: Set<String>,
    features: Map<String, PaperFeature.Profile>,
    val featureConfigs: Map<String, ConfigurationNode>,
    slots: Map<String, PaperSlot>,
    stats: List<ApplicableStats>,
) : AbstractComponent<PaperComponent, PaperFeature.Profile, PaperSlot>(id, tags, features, slots, stats)

class PaperSlot(
    key: String,
    required: Boolean,
    rule: Rule,
) : SimpleSlot(key, required, rule)

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

    override fun copy(
        component: PaperComponent,
        features: Map<String, PaperFeature.Data>,
        parent: PaperNodeKey?,
        children: Map<String, PaperDataNode>
    ) = PaperDataNode(
        component,
        features.map { (key, value) -> key to value.copy() }.associate { it }.toMutableMap(),
        legacyFeatures,
        parent,
        children.map { (key, value) -> key to value.copy() }.associate { it }.toMutableMap(),
        legacyChildren
    )

    override fun copy(): PaperDataNode = copy(component = component)

    fun backedCopy(platform: Sokol): PaperDataNode? {
        if (parent != null)
            throw IllegalStateException(errorMsg("Can only reload from backing platform on a root node"))

        fun PaperDataNode.copy(parent: PaperNodeKey?): PaperDataNode? {
            return platform.components[component.id]?.let { component ->
                val res = PaperDataNode(
                    component,
                    HashMap(),
                    legacyFeatures,
                    parent,
                    HashMap(),
                    legacyChildren
                )
                features.forEach { (key, data) ->
                    // serialize data into a tag; get the new profile from our component; deserialize the data
                    val dataTag = platform.persistence.newTag().apply { data.serialize(this) }
                    component.features[key]?.let { profile ->
                        try {
                            res.features[key] = profile.createData(dataTag)
                        } catch (ex: Exception) {
                            // go down to the `run` block and save as legacy
                            null
                        }
                    } ?: run {
                        // if it can't be deserialized again, it gets saved as a legacy feature
                        res.legacyFeatures[key] = dataTag
                    }
                }
                children.forEach { (key, child) ->
                    // if a child can't be copied (its component is no longer real),
                    // it gets saved as a legacy child instead
                    child.copy(PaperNodeKey(this, key))?.let { copy ->
                        res.children[key] = copy
                    } ?: run {
                        res.legacyChildren[key] = platform.persistence.newTag().apply { child.serialize(this) }
                    }
                }
                res
            }
        }

        return copy(null)
    }
}

class PaperTreeState(
    root: PaperDataNode,
    stats: CompiledStatMap,
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
                override val state: PaperTreeState get() = this@PaperTreeState
                override val host: PaperNodeHost get() = host
                override val node: PaperDataNode get() = node

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

fun paperStateOf(root: PaperDataNode): PaperTreeState {
    val (stats, nodeStates, incomplete) = treeStateData(root)
    return PaperTreeState(root, stats, nodeStates, incomplete)
}
