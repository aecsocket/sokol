package com.github.aecsocket.sokol.paper

import com.github.aecsocket.sokol.core.NodeKey
import com.github.aecsocket.sokol.core.TreeState
import com.github.aecsocket.sokol.core.impl.AbstractDataNode
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataContainer

typealias PaperNodeKey = NodeKey<PaperDataNode>

class PaperDataNode(
    value: PaperComponent,
    features: MutableMap<String, PaperFeature.Data> = HashMap(),
    val legacyFeatures: MutableMap<NamespacedKey, PersistentDataContainer> = HashMap(),
    parent: PaperNodeKey? = null,
    children: MutableMap<String, PaperDataNode> = HashMap(),
    val legacyChildren: MutableMap<NamespacedKey, PersistentDataContainer> = HashMap()
) : AbstractDataNode<PaperDataNode, PaperComponent, PaperFeature.Data>(value, features, parent, children) {
    override val self = this

    fun createState() = TreeState.from(this)

    override fun copy(): PaperDataNode = PaperDataNode(
        value,
        features.map { (key, value) -> key to value.copy() }.associate { it }.toMutableMap(),
        legacyFeatures,
        parent,
        children.map { (key, value) -> key to value.copy() }.associate { it }.toMutableMap(),
        legacyChildren
    )
}
