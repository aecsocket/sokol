package com.github.aecsocket.sokol.paper

import com.github.aecsocket.sokol.core.NodeKey
import com.github.aecsocket.sokol.core.impl.AbstractDataNode
import com.github.aecsocket.sokol.core.nbt.BinaryTag
import com.github.aecsocket.sokol.core.nbt.CompoundBinaryTag

typealias PaperNodeKey = NodeKey<PaperDataNode>

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
        tag.setCompound(FEATURES) {
            legacyFeatures.forEach { (key, tag) -> set(key, tag) }
            features.forEach { (key, feature) ->
                setCompound(key) { feature.serialize(this) }
            }
        }
        tag.setCompound(CHILDREN) {
            legacyChildren.forEach { (key, tag) -> set(key, tag) }
            children.forEach { (key, child) ->
                setCompound(key) { child.serialize(this) }
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
