package com.github.aecsocket.sokol.paper

import com.github.aecsocket.alexandria.paper.extension.forEach
import com.github.aecsocket.alexandria.paper.extension.force
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataHolder
import org.bukkit.persistence.PersistentDataType

class SokolPersistence internal constructor(
    val plugin: SokolPlugin
) {
    private val keyId = plugin.key("id")
    private val keyFeatures = plugin.key("features")
    private val keyChildren = plugin.key("children")
    private val dataType = DataType()

    private inner class DataType : PersistentDataType<PersistentDataContainer, PaperDataNode> {
        override fun getPrimitiveType() = PersistentDataContainer::class.java
        override fun getComplexType() = PaperDataNode::class.java

        override fun toPrimitive(
            obj: PaperDataNode,
            ctx: PersistentDataAdapterContext
        ): PersistentDataContainer {
            val pdc = ctx.newPersistentDataContainer()

            pdc[keyId, PersistentDataType.STRING] = obj.value.id

            val features = ctx.newPersistentDataContainer()
            obj.legacyFeatures.forEach { (key, value) ->
                features[key, PersistentDataType.TAG_CONTAINER] = value
            }
            obj.features.forEach { (key, feature) ->
                features[plugin.key(key), PersistentDataType.TAG_CONTAINER] = feature.serialize(ctx)
            }
            pdc[keyFeatures, PersistentDataType.TAG_CONTAINER] = features

            val children = ctx.newPersistentDataContainer()
            obj.legacyChildren.forEach { (key, value) ->
                children[key, PersistentDataType.TAG_CONTAINER] = value
            }
            obj.children.forEach { (key, child) ->
                children[plugin.key(key), PersistentDataType.TAG_CONTAINER] = toPrimitive(child, ctx)
            }
            pdc[keyChildren, PersistentDataType.TAG_CONTAINER] = children

            return pdc
        }

        override fun fromPrimitive(
            pdc: PersistentDataContainer,
            ctx: PersistentDataAdapterContext
        ): PaperDataNode {
            fun fromPrimitive0(pdc: PersistentDataContainer, parent: PaperNodeKey?): PaperDataNode {
                val id = pdc.force(keyId, PersistentDataType.STRING)
                val value = plugin.components[id]
                    ?: throw IllegalArgumentException("No component with ID '$id'")

                val features = HashMap<String, PaperFeature.Data>()
                val legacyFeatures = HashMap<NamespacedKey, PersistentDataContainer>()
                pdc.force(keyFeatures, PersistentDataType.TAG_CONTAINER)
                    .forEach(PersistentDataType.TAG_CONTAINER) { key, data ->
                        value.features[key.value()]?.let { profile ->
                            features[key.toString()] = profile.deserialize(data)
                        } ?: run { legacyFeatures[key] = data }
                    }

                val children = HashMap<String, PaperDataNode>()
                val legacyChildren = HashMap<NamespacedKey, PersistentDataContainer>()
                val res = PaperDataNode(value, features, legacyFeatures, parent, children, legacyChildren)

                pdc.force(keyChildren, PersistentDataType.TAG_CONTAINER)
                    .forEach(PersistentDataType.TAG_CONTAINER) { key, data ->
                        try {
                            children[key.value()] = fromPrimitive0(data, parent)
                        } catch (ex: IllegalArgumentException) {
                            legacyChildren[key] = data
                        }
                    }

                //println(" > $id feat keys = |${pdc.force(keyFeatures, PersistentDataType.TAG_CONTAINER).keys.size}| feats = |${features.size}| legacy feats = |${legacyFeatures.size}|")
                return res
            }

            return fromPrimitive0(pdc, null)
        }
    }

    fun isNode(pdc: PersistentDataContainer) = pdc.has(plugin.keyNode)

    fun get(pdc: PersistentDataContainer) = pdc.get(plugin.keyNode, dataType)

    fun getStack(stack: ItemStack?) = stack?.itemMeta?.let { get(it.persistentDataContainer) }

    fun set(pdc: PersistentDataContainer, node: PaperDataNode) = pdc.set(plugin.keyNode, dataType, node)

    fun setTick(pdc: PersistentDataContainer) = pdc.set(plugin.keyTick, PersistentDataType.BYTE, 0)
}
