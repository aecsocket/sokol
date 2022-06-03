package com.github.aecsocket.sokol.paper

import com.github.aecsocket.sokol.core.NodeKey
import com.github.aecsocket.sokol.core.rule.Rule
import com.github.aecsocket.sokol.core.serializer.BlueprintSerializer
import com.github.aecsocket.sokol.core.serializer.ComponentSerializer
import com.github.aecsocket.sokol.core.serializer.DataNodeSerializer
import com.github.aecsocket.sokol.paper.*
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get

private const val TAGS = "tags"
private const val REQUIRED = "required"
private const val MODIFIABLE = "modifiable"
private const val RULE = "rule"

class PaperComponentSerializer(
    private val plugin: SokolPlugin
) : ComponentSerializer<
    PaperComponent, PaperFeature, PaperFeature.Profile, PaperSlot
>() {
    override fun feature(id: String) = plugin.features[id]

    override fun slot(key: String, node: ConfigurationNode) = PaperSlot(
        key,
        node.node(TAGS).get<MutableSet<String>> { HashSet() },
        node.node(REQUIRED).getBoolean(false),
        node.node(MODIFIABLE).getBoolean(false),
        Rule.Temp
    )

    override fun create(
        id: String,
        features: Map<String, PaperFeature.Profile>,
        slots: Map<String, PaperSlot>,
        tags: Set<String>
    ) = PaperComponent(id, features, slots, tags)
}

class PaperNodeSerializer(
    private val plugin: SokolPlugin
) : DataNodeSerializer<
    PaperDataNode, PaperComponent, PaperFeature.Profile, PaperFeature.Data
>() {
    override fun component(id: String) = plugin.components[id]

    override fun create(
        value: PaperComponent,
        features: MutableMap<String, PaperFeature.Data>,
        parent: NodeKey<PaperDataNode>?,
        children: MutableMap<String, PaperDataNode>
    ) = PaperDataNode(value, features = features, parent = parent, children = children)
}

class PaperBlueprintSerializer(
    private val plugin: SokolPlugin
) : BlueprintSerializer<
    PaperBlueprint, PaperDataNode
>() {
    override fun create(
        id: String,
        node: PaperDataNode
    ) = PaperBlueprint(id, node)

    override val nodeType: Class<PaperDataNode>
        get() = PaperDataNode::class.java
}
