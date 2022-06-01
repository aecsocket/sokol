package com.github.aecsocket.sokol.paper

import com.github.aecsocket.sokol.core.NodePath
import com.github.aecsocket.sokol.core.keyOf
import com.github.aecsocket.sokol.core.nbt.BinaryTag
import com.github.aecsocket.sokol.core.nbt.CompoundBinaryTag
import com.github.aecsocket.sokol.core.nbt.TagSerializationException
import com.github.aecsocket.sokol.core.stat.StatMap
import net.minecraft.nbt.CompoundTag
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack
import org.bukkit.craftbukkit.v1_18_R2.persistence.CraftPersistentDataContainer
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataContainer

internal const val ID = "id"
internal const val FEATURES = "features"
internal const val CHILDREN = "children"

class SokolPersistence internal constructor(
    private val plugin: SokolPlugin
) {
    private val nodeKey = plugin.key("tree").asString()

    fun stateFrom(tag: CompoundBinaryTag): PaperTreeState {
        val stats = object : StatMap {} // todo
        val incomplete = ArrayList<NodePath>()
        val featureStates = HashMap<PaperDataNode, Map<String, PaperFeature.State>>()

        fun get0(tag: CompoundBinaryTag, parent: PaperNodeKey?, path: NodePath): PaperDataNode {
            val id = tag.forceString(ID)
            val component = plugin.components[id]
                ?: throw TagSerializationException("No component with ID '$id'")

            val featureData = HashMap<String, PaperFeature.Data>()
            val featureState = HashMap<String, PaperFeature.State>()
            val legacyFeatures = HashMap<String, BinaryTag>()

            val featuresLeft = component.features.toMutableMap()
            tag.getCompound(FEATURES)?.forEach { (key, tag) ->
                featuresLeft.remove(key)?.let { profile ->
                    try {
                        val feature = profile.createData(tag as CompoundBinaryTag)
                        featureData[key] = feature
                        featureState[key] = feature.createState()
                    } catch (ex: TagSerializationException) {
                        legacyFeatures[key] = tag
                    }
                } ?: run { legacyFeatures[key] = tag } // this feature does not have a profile on the component
            }
            // all the non-stated features will have their state generated...
            featuresLeft.forEach { (key, profile) ->
                featureState[key] = profile.createData().createState()
            }

            featureState.forEach { (_, state) ->
                state.resolveDependencies(featureState::get)
            }

            val children = HashMap<String, PaperDataNode>()
            val legacyChildren = HashMap<String, BinaryTag>()
            val node = PaperDataNode(component, featureData, legacyFeatures, parent, children, legacyChildren)

            featureStates[node] = featureState

            val slotsLeft = component.slots.toMutableMap()
            tag.getCompound(CHILDREN)?.forEach { (key, tag) ->
                if (slotsLeft.remove(key) == null) {
                    // this child is not for a valid slot
                    legacyChildren[key] = tag
                } else {
                    try {
                        children[key] = get0(tag as CompoundBinaryTag, node.keyOf(key), path + key)
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

    fun newTag(): CompoundBinaryTag.Mutable = PaperCompoundTag(CompoundTag())

    fun readFromData(pdc: PersistentDataContainer): CompoundBinaryTag.Mutable? {
        return (pdc as CraftPersistentDataContainer).raw[nodeKey]?.let { PaperCompoundTag(it as CompoundTag) }
    }

    fun writeToData(tag: CompoundBinaryTag, pdc: PersistentDataContainer) {
        (pdc as CraftPersistentDataContainer).raw[nodeKey] = (tag as PaperCompoundTag).handle
    }

    fun writeToStack(tag: CompoundBinaryTag, stack: ItemStack): ItemStack {
        val res = if (stack is CraftItemStack) stack else CraftItemStack.asCraftCopy(stack)
        val nms = res.handle
        nms.tag = (nms.tag ?: CompoundTag()).apply {
            put(nodeKey, (tag as PaperCompoundTag).handle)
        }
        return res
    }
}
