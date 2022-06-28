package com.github.aecsocket.sokol.paper

import com.github.aecsocket.sokol.core.NodePath
import com.github.aecsocket.sokol.core.SokolPersistence
import com.github.aecsocket.sokol.core.emptyNodePath
import com.github.aecsocket.sokol.core.keyOf
import com.github.aecsocket.sokol.core.nbt.BinaryTag
import com.github.aecsocket.sokol.core.nbt.CompoundBinaryTag
import com.github.aecsocket.sokol.core.nbt.TagSerializationException
import net.minecraft.nbt.ByteTag
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NumericTag
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack
import org.bukkit.craftbukkit.v1_18_R2.persistence.CraftPersistentDataContainer
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataContainer
import org.spongepowered.configurate.ConfigurateException
import org.spongepowered.configurate.kotlin.extensions.get
import java.io.BufferedWriter
import java.io.StringWriter

internal const val VERSION = "version"
internal const val ID = "id"
internal const val FEATURES = "features"
internal const val CHILDREN = "children"
internal const val BUKKIT_PDC = "PublicBukkitValues"
const val NODE_VERSION = 1

class PaperPersistence internal constructor(
    private val plugin: SokolPlugin
) : SokolPersistence<PaperDataNode> {
    internal val keyNode = plugin.keyNode.asString()
    internal val keyTick = plugin.keyTick.asString()

    fun newTag(): CompoundBinaryTag.Mutable = PaperCompoundTag(CompoundTag())

    // Tag/node

    fun tagToNode(tag: CompoundBinaryTag): PaperDataNode {
        fun get0(tag: CompoundBinaryTag, parent: PaperNodeKey?, path: NodePath): PaperDataNode {
            val id = tag.forceString(ID)
            val component = plugin.components[id]
                ?: throw TagSerializationException("No component with ID '$id'")

            val featureData = HashMap<String, PaperFeature.Data>()
            val legacyFeatures = HashMap<String, BinaryTag>()

            val featuresLeft = component.features.toMutableMap()
            tag.getCompound(FEATURES)?.forEach { (key, tag) ->
                featuresLeft.remove(key)?.let { profile ->
                    try {
                        val feature = profile.createData(tag as CompoundBinaryTag)
                        featureData[key] = feature
                    } catch (ex: TagSerializationException) {
                        // couldn't deserialize this feature successfully, it's legacy
                        legacyFeatures[key] = tag
                    }
                } ?: run {
                    // this feature does not have a profile on the component
                    legacyFeatures[key] = tag
                }
            }

            val children = HashMap<String, PaperDataNode>()
            val legacyChildren = HashMap<String, BinaryTag>()
            val node = PaperDataNode(component, featureData, legacyFeatures, parent, children, legacyChildren)

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

            return node
        }

        return get0(tag, null, emptyNodePath())
    }

    fun nodeToTag(node: PaperDataNode, tag: CompoundBinaryTag.Mutable) {
        node.serialize(tag)
        tag.setInt(VERSION, NODE_VERSION)
    }

    // Tag/state

    fun stateToTag(state: PaperTreeState, tag: CompoundBinaryTag.Mutable) {
        nodeToTag(state.updatedRoot(), tag)
    }

    // Tag/PDC

    fun dataToTag(pdc: PersistentDataContainer): CompoundBinaryTag.Mutable? {
        return (pdc as CraftPersistentDataContainer).raw[keyNode]?.let { PaperCompoundTag(it as CompoundTag) }
    }

    fun tagToData(tag: CompoundBinaryTag, pdc: PersistentDataContainer) {
        (pdc as CraftPersistentDataContainer).raw[keyNode] = (tag as PaperCompoundTag).handle
    }

    // Tag/stack

    fun stackToTag(stack: ItemStack): CompoundBinaryTag.Mutable? {
        return if (stack is CraftItemStack) {
            stack.handle.tag?.let { tag ->
                tag.getCompound(BUKKIT_PDC).tags[keyNode]?.let { PaperCompoundTag(it as CompoundTag) }
            }
        } else null
    }

    // String/node

    override fun stringToNode(string: String): PaperDataNode {
        return plugin.loaderBuilder().buildAndLoadString(string).get<PaperDataNode>()
            ?: throw ConfigurateException("Null node created")
    }

    override fun nodeToString(node: PaperDataNode): String {
        val writer = StringWriter()
        plugin.loaderBuilder().sink { BufferedWriter(writer) }.build().apply {
            save(createNode().set(node))
        }
        return writer.toString()
    }

    /*
    // avoid ItemMeta gets/sets
    fun tagToStack(tag: CompoundBinaryTag, stack: ItemStack): ItemStack {
        val res = if (stack is CraftItemStack) stack else CraftItemStack.asCraftCopy(stack)
        val nms = res.handle
        nms.tag = (nms.tag ?: CompoundTag()).apply {
            tags[BUKKIT_PDC] = (tags[BUKKIT_PDC] ?: CompoundTag()).apply {
                (this as CompoundTag).tags[keyNode] = (tag as PaperCompoundTag).handle
            }
        }
        return res
    }*/

    fun setTicks(value: Boolean, pdc: PersistentDataContainer) {
        (pdc as CraftPersistentDataContainer).raw[keyTick] = ByteTag.valueOf(value)
    }

    fun doesTick(pdc: PersistentDataContainer): Boolean {
        return (pdc as CraftPersistentDataContainer).raw[keyTick]?.let {
            it is NumericTag && it.asByte != (0).toByte()
        } == true
    }

    /*
    fun nodeToTag(node: PaperDataNode, tag: CompoundBinaryTag.Mutable) {
        node.serialize(tag)
        tag.setInt(VERSION, NODE_VERSION)
    }

    fun stateToTag(state: PaperTreeState, tag: CompoundBinaryTag.Mutable) {
        nodeToTag(state.updatedRoot(), tag)
    }


    fun dataToTag(pdc: PersistentDataContainer): CompoundBinaryTag.Mutable? {
        return (pdc as CraftPersistentDataContainer).raw[keyNode]?.let { PaperCompoundTag(it as CompoundTag) }
    }

    fun tagToData(tag: CompoundBinaryTag, pdc: PersistentDataContainer) {
        (pdc as CraftPersistentDataContainer).raw[keyNode] = (tag as PaperCompoundTag).handle
    }

    private fun dataTag(stack: ItemStack): Pair<CompoundTag, ItemStack> {
        val res = if (stack is CraftItemStack) stack else CraftItemStack.asCraftCopy(stack)
        val nms = res.handle
        nms.tag = (nms.tag ?: CompoundTag()).let {
            it.tags[BUKKIT_PDC] ?: CompoundTag()
        }

    }

    // avoid ItemMeta gets/sets
    fun tagToStack(tag: CompoundBinaryTag, stack: ItemStack): ItemStack {
        val res = if (stack is CraftItemStack) stack else CraftItemStack.asCraftCopy(stack)
        val nms = res.handle
        nms.tag = (nms.tag ?: CompoundTag()).apply {
            tags[BUKKIT_PDC] = (tags[BUKKIT_PDC] ?: CompoundTag()).apply {
                (this as CompoundTag).tags[keyNode] = (tag as PaperCompoundTag).handle
            }
        }
        return res
    }

    fun nodeToStack(node: PaperDataNode, stack: ItemStack) =
        tagToStack(newTag().apply { nodeToTag(node, this) }, stack)

    fun stateToStack(state: PaperTreeState, stack: ItemStack) =
        tagToStack(newTag().apply { stateToTag(state, this) }, stack)

    fun setTicks(value: Boolean, pdc: PersistentDataContainer) {
        (pdc as CraftPersistentDataContainer).raw[keyTick] = ByteTag.valueOf(value)
    }

    fun setTicks(value: Boolean, stack: ItemStack) {

    }

    fun ticks(pdc: PersistentDataContainer) = (pdc as CraftPersistentDataContainer).raw[keyTick]?.let {
        it is NumericTag && it.asByte != (0).toByte()
    }*/
}
