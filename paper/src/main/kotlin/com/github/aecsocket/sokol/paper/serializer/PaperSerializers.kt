package com.github.aecsocket.sokol.paper.serializer

import com.github.aecsocket.alexandria.core.extension.force
import com.github.aecsocket.sokol.core.NodeKey
import com.github.aecsocket.sokol.core.rule.Rule
import com.github.aecsocket.sokol.core.serializer.ComponentSerializer
import com.github.aecsocket.sokol.core.serializer.DataNodeSerializer
import com.github.aecsocket.sokol.paper.*
import org.spongepowered.configurate.ConfigurationNode

class PaperComponentSerializer(
    private val plugin: SokolPlugin
) : ComponentSerializer<
    PaperComponent, PaperFeature, PaperFeature.Profile, PaperSlot
>() {
    override fun feature(id: String) = plugin.features[id]

    override fun slot(key: String, node: ConfigurationNode) = PaperSlot(
        key, node.force(), Rule.Temp
    )

    override fun create(
        id: String,
        features: Map<String, PaperFeature.Profile>,
        slots: Map<String, PaperSlot>
    ) = PaperComponent(id, features, slots)
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
