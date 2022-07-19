package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.core.keyed.RegistryRef
import com.gitlab.aecsocket.alexandria.core.keyed.by
import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.NodePath
import com.gitlab.aecsocket.sokol.core.SokolPersistence
import com.gitlab.aecsocket.sokol.core.emptyNodePath
import com.gitlab.aecsocket.sokol.core.feature.HostCreationException
import com.gitlab.aecsocket.sokol.core.keyOf
import com.gitlab.aecsocket.sokol.core.nbt.BinaryTag
import com.gitlab.aecsocket.sokol.core.nbt.CompoundBinaryTag
import com.gitlab.aecsocket.sokol.core.nbt.TagSerializationException
import com.gitlab.aecsocket.sokol.paper.feature.PaperItemHoster
import net.kyori.adventure.key.Key
import net.minecraft.nbt.ByteTag
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NumericTag
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack
import org.bukkit.craftbukkit.v1_19_R1.persistence.CraftPersistentDataContainer
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

class ComponentRef(value: PaperComponent) : RegistryRef<PaperComponent>(value)

class BlueprintRef(value: PaperBlueprint) : RegistryRef<PaperBlueprint>(value)

class FeatureRef(value: PaperFeature) : RegistryRef<PaperFeature>(value)

class PaperPersistence internal constructor(
    private val plugin: Sokol
) : SokolPersistence<PaperDataNode> {
    val kNode = plugin.key("node")
    val kTick = plugin.key("tick")

    internal val sNode = kNode.asString()
    internal val sTick = kTick.asString()

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

    // Tags

    fun newTag(): CompoundBinaryTag.Mutable = PaperCompoundTag(CompoundTag())

    fun tagTo(tag: CompoundBinaryTag, pdc: PersistentDataContainer, key: String) {
        (pdc as CraftPersistentDataContainer).raw[key] = (tag as PaperBinaryTag).handle
    }

    fun tagTo(tag: CompoundBinaryTag, pdc: PersistentDataContainer, key: Key) =
        tagTo(tag, pdc, key.asString())

    fun nodeTagTo(tag: CompoundBinaryTag, pdc: PersistentDataContainer) =
        tagTo(tag, pdc, sNode)

    // Tag/node

    fun nodeOf(tag: CompoundBinaryTag): PaperDataNode? {
        fun get0(tag: CompoundBinaryTag, parent: PaperNodeKey?, path: NodePath): PaperDataNode? {
            val id = tag.forceString(ID)
            val component = plugin.components[id] ?: return null

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
                        get0(tag as CompoundBinaryTag, node.keyOf(key), path + key)?.let { children[key] = it }
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

    fun nodeInto(node: PaperDataNode, tag: CompoundBinaryTag.Mutable) {
        node.serialize(tag)
        tag.setInt(VERSION, NODE_VERSION)
    }

    fun stateInto(state: PaperTreeState, tag: CompoundBinaryTag.Mutable) {
        nodeInto(state.updatedRoot(), tag)
    }



    fun tagOf(stack: ItemStack, key: String): CompoundBinaryTag.Mutable? {
        return if (stack is CraftItemStack) {
            stack.handle?.tag?.let { tag ->
                tag.getCompound(BUKKIT_PDC).tags[key]?.let { PaperCompoundTag(it as CompoundTag) }
            }
        } else null
    }

    fun tagOf(stack: ItemStack, key: Key) = tagOf(stack, key.asString())

    fun nodeTagOf(stack: ItemStack) = tagOf(stack, sNode)


    fun tagOf(pdc: PersistentDataContainer, key: String): CompoundBinaryTag.Mutable? {
        return (pdc as CraftPersistentDataContainer).raw[key]?.let { PaperCompoundTag(it as CompoundTag) }
    }

    fun tagOf(pdc: PersistentDataContainer, key: Key) = tagOf(pdc, key.asString())

    fun nodeTagOf(pdc: PersistentDataContainer) = tagOf(pdc, sNode)

    fun forceTagOf(pdc: PersistentDataContainer, key: String): CompoundBinaryTag.Mutable {
        return PaperCompoundTag((pdc as CraftPersistentDataContainer).raw.computeIfAbsent(key) { CompoundTag() } as CompoundTag)
    }

    fun forceTagOf(pdc: PersistentDataContainer, key: Key) = forceTagOf(pdc, key.asString())

    fun forceNodeTagOf(pdc: PersistentDataContainer) = forceTagOf(pdc, sNode)

    // Stacks

    fun stateToStack(holder: PaperItemHolder, state: PaperTreeState): ItemStack? {
        val node = state.root
        val feature = plugin.settings.hostFeatures.item.value
        val itemHosted = state.nodeStates[node]?.by<PaperItemHoster>(feature)
            ?: return null

        return itemHosted.itemHosted(holder, state).stack
    }

    fun forceStateToStack(holder: PaperItemHolder, state: PaperTreeState) = stateToStack(holder, state)
        ?: throw HostCreationException("Node does not have hoster '${plugin.settings.hostFeatures.item.value.id}'")

    // Ticks

    fun setTicks(value: Boolean, pdc: PersistentDataContainer) {
        (pdc as CraftPersistentDataContainer).raw[sTick] = ByteTag.valueOf(value)
    }

    fun ticks(pdc: PersistentDataContainer): Boolean {
        return (pdc as CraftPersistentDataContainer).raw[sTick]?.let {
            it is NumericTag && it.asByte != (0).toByte()
        } == true
    }
}
