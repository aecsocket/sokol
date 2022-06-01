package com.github.aecsocket.sokol.paper

import com.github.aecsocket.sokol.core.NodePath
import com.github.aecsocket.sokol.core.keyOf
import com.github.aecsocket.sokol.core.nbt.BinaryTag
import com.github.aecsocket.sokol.core.nbt.CompoundBinaryTag
import com.github.aecsocket.sokol.core.nbt.TagSerializationException
import com.github.aecsocket.sokol.core.stat.StatMap
import net.minecraft.nbt.CompoundTag
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack
import org.bukkit.inventory.ItemStack

internal const val ID = "id"
internal const val FEATURES = "features"
internal const val CHILDREN = "children"

class SokolPersistence internal constructor(
    private val plugin: SokolPlugin
) {
    private val nodeKey = plugin.key("tree").asString()

    fun stateOf(tag: CompoundBinaryTag.Mutable): PaperTreeState {
        val stats = object : StatMap {} // todo
        val incomplete = ArrayList<NodePath>()
        val featureStates = HashMap<PaperDataNode, NodeState>()

        fun get0(tag: CompoundBinaryTag.Mutable, parent: PaperNodeKey?, path: NodePath): PaperDataNode {
            val id = tag.forceString(ID)
            val component = plugin.components[id]
                ?: throw TagSerializationException("No component with ID '$id'")

            val features = HashMap<String, PaperFeature.Data>()
            val legacyFeatures = HashMap<String, BinaryTag>()
            val nodeFeatures = HashMap<String, Pair<PaperFeature.State, CompoundBinaryTag.Mutable>>()

            val featuresLeft = component.features.toMutableMap()
            val tagFeatures = tag.getOrEmpty(FEATURES)
            tagFeatures.forEach { (key, tag) ->
                featuresLeft.remove(key)?.let { profile ->
                    try {
                        val feature = profile.deserialize(tag as CompoundBinaryTag)
                        features[key] = feature
                        nodeFeatures[key] = feature.createState() to tag as CompoundBinaryTag.Mutable
                    } catch (ex: TagSerializationException) {
                        legacyFeatures[key] = tag
                    }
                } ?: run { legacyFeatures[key] = tag } // this feature does not have a profile on the component
            }
            println("features left = $featuresLeft")
            // all the non-stated features will have their state generated...
            featuresLeft.forEach { (key, profile) ->
                nodeFeatures[key] = profile.createData().createState() to tagFeatures.getOrEmpty(key)
            }

            nodeFeatures.forEach { (_, feature) ->
                feature.first.resolveDependencies { nodeFeatures[it]?.first }
            }

            val children = HashMap<String, PaperDataNode>()
            val legacyChildren = HashMap<String, BinaryTag>()
            val node = PaperDataNode(component, features, legacyFeatures, parent, children, legacyChildren)

            featureStates[node] = NodeState(tag, nodeFeatures.values)

            val slotsLeft = component.slots.toMutableMap()
            tag.getCompound(CHILDREN)?.forEach { (key, tag) ->
                if (slotsLeft.remove(key) == null) {
                    // this child is not for a valid slot
                    legacyChildren[key] = tag
                } else {
                    try {
                        children[key] = get0(tag as CompoundBinaryTag.Mutable, node.keyOf(key), path + key)
                    } catch (ex: TagSerializationException) {
                        // couldn't deserialize this child successfully, it's legacy
                        legacyChildren[key] = tag
                    }
                }
            }
            // all the non-filled slots will be checked...
            slotsLeft.forEach { (key, slot) ->
                if (slot.required) {
                    incomplete.add(path + key)
                }
            }

            return node
        }

        return PaperTreeState(
            get0(tag, null, NodePath.EMPTY),
            stats,
            featureStates,
            incomplete
        )
    }

    fun stateOf(stack: ItemStack?): PaperTreeState? {
        return stack?.let {
            PaperBinaryTags.fromStack(stack)?.getCompound(nodeKey)?.let {
                stateOf(it)
            }
        }
    }

    fun writeTo(node: PaperDataNode, stack: ItemStack): ItemStack {
        val tag = NMSCompoundTag(CompoundTag())
        node.serialize(tag)

        val res = if (stack is CraftItemStack) stack else CraftItemStack.asCraftCopy(stack)
        val nms = res.handle
        nms.tag = (nms.tag ?: CompoundTag()).apply {
            put(nodeKey, tag.nms)
        }
        return res
    }
}
