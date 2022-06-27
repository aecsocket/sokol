package com.github.aecsocket.sokol.paper

import com.github.aecsocket.sokol.core.NodeKey
import com.github.aecsocket.sokol.core.rule.Rule
import com.github.aecsocket.sokol.core.serializer.BlueprintSerializer
import com.github.aecsocket.sokol.core.serializer.ComponentSerializer
import com.github.aecsocket.sokol.core.serializer.DataNodeSerializer
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import java.lang.reflect.Type

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
        node.node(RULE).get<Rule> { Rule.True }
    )

    override fun deserialize(type: Type, node: ConfigurationNode) = PaperComponent(
        id(type, node),
        features(type, node),
        node.node(FEATURES).childrenMap()
            .map { (key, child) -> key.toString() to child }
            .associate { it },
        slots(type, node),
        tags(type, node)
    )
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
    override val nodeType: Class<PaperDataNode>
        get() = PaperDataNode::class.java

    override fun deserialize(type: Type, node: ConfigurationNode) = PaperBlueprint(
        id(type, node),
        node(type, node)
    )
}
