package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.core.extension.force
import com.gitlab.aecsocket.sokol.core.NodeKey
import com.gitlab.aecsocket.sokol.core.rule.Rule
import com.gitlab.aecsocket.sokol.core.serializer.BlueprintSerializer
import com.gitlab.aecsocket.sokol.core.serializer.ComponentSerializer
import com.gitlab.aecsocket.sokol.core.serializer.DataNodeSerializer
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.reflect.Type

private const val TAGS = "tags"
private const val SOFT_FEATURES = "soft_features"
private const val REQUIRED = "required"
private const val MODIFIABLE = "modifiable"
private const val RULE = "rule"

class PaperComponentSerializer(
    private val plugin: Sokol
) : ComponentSerializer<
        PaperComponent, PaperFeature, PaperFeature.Profile, PaperSlot
>() {
    override fun feature(id: String) = plugin.features[id]

    override fun slot(key: String, node: ConfigurationNode) = PaperSlot(
        key,
        node.node(TAGS).get<MutableSet<String>> { HashSet() },
        node.node(REQUIRED).getBoolean(false),
        node.node(RULE).get<Rule> { Rule.True },
        node.node(MODIFIABLE).getBoolean(false),
    )

    override fun deserialize(type: Type, node: ConfigurationNode): PaperComponent {
        val featureDeps =
            features(type, node).map { (_, profile) -> profile.type } +
            node.node(SOFT_FEATURES).childrenList().map {
                val id = it.force<String>()
                plugin.features[id] ?: throw SerializationException(it, type, "No feature with ID '$id'")
            }
        plugin.statMapSerializer.types = featureStatTypes(featureDeps)
        plugin.ruleSerializer.types = featureRuleTypes(featureDeps)
        val res = PaperComponent(
            id(type, node),
            tags(type, node),
            features(type, node),
            node.node(FEATURES).childrenMap()
                .map { (key, child) -> key.toString() to child }
                .associate { it },
            slots(type, node),
            stats(type, node),
        )
        plugin.statMapSerializer.types = emptyMap()
        plugin.ruleSerializer.types = emptyMap()
        return res
    }
}

class PaperNodeSerializer(
    private val plugin: Sokol
) : DataNodeSerializer<
        PaperDataNode, PaperComponent, PaperFeature.Profile, PaperFeature.Data
>() {
    override fun component(id: String) = plugin.components[id]

    override fun create(
        value: PaperComponent,
        features: MutableMap<String, PaperFeature.Data>,
        parent: PaperNodeKey?,
        children: MutableMap<String, PaperDataNode>
    ) = PaperDataNode(value, features = features, parent = parent, children = children)
}

class PaperBlueprintSerializer(
    private val plugin: Sokol
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
