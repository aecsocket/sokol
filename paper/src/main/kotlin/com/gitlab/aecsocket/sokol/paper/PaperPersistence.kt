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

interface TagHolder {
    fun contains(key: Key): Boolean

    operator fun get(key: Key): CompoundBinaryTag.Mutable?

    operator fun set(key: Key, tag: CompoundBinaryTag)

    fun remove(key: Key)
}

class PaperPersistence internal constructor(
    private val plugin: Sokol
) : SokolPersistence<PaperDataNode> {
    val kNode = plugin.key("node")
    val kTick = plugin.key("tick")

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

    fun holderOf(pdc: PersistentDataContainer) = object : TagHolder {
        override fun contains(key: Key): Boolean {
            return (pdc as CraftPersistentDataContainer).raw.contains(key.asString())
        }

        override fun get(key: Key): CompoundBinaryTag.Mutable? {
            return (pdc as CraftPersistentDataContainer).raw[key.asString()]?.let {
                PaperCompoundTag(it as CompoundTag)
            }
        }

        override fun set(key: Key, tag: CompoundBinaryTag) {
            (pdc as CraftPersistentDataContainer).raw[key.asString()] = (tag as PaperCompoundTag).handle
        }

        override fun remove(key: Key) {
            (pdc as CraftPersistentDataContainer).raw.remove(key.asString())
        }
    }

    fun holderOf(stack: ItemStack) = object : TagHolder {
        override fun contains(key: Key): Boolean {
            return (stack as? CraftItemStack)?.handle?.tag?.contains(key.asString()) == true
        }

        override fun get(key: Key): CompoundBinaryTag.Mutable? {
            return (stack as? CraftItemStack)?.handle?.tag?.let { tag ->
                tag.getCompound(BUKKIT_PDC).tags[key.asString()]?.let {
                    PaperCompoundTag(it as CompoundTag)
                }
            }
        }

        override fun set(key: Key, tag: CompoundBinaryTag) {
            stack.editMeta { meta ->
                holderOf(meta.persistentDataContainer)[key] = tag
            }
        }

        override fun remove(key: Key) {
            (stack as? CraftItemStack)?.handle?.tag?.remove(key.asString())
        }
    }

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

    fun useStack(stack: ItemStack, holder: PaperItemHolder, action: (PaperTreeState, PaperItemHost) -> Unit) {
        holderOf(stack)[kNode]?.let { tag ->
            nodeOf(tag)?.let { node ->
                val state = paperStateOf(node)
                plugin.useHostOf(holder, { stack },
                    action = { action(state, it) },
                    onDirty = { meta ->
                        // full update; write meta
                        //  · we write into PDC first, so when meta is set onto stack,
                        //    meta's PDC is written into stack's tag
                        //  · we can't just write meta then apply to our local `tag`,
                        //    because setItemMeta overwrites the stack's CompoundTag
                        holderOf(meta.persistentDataContainer)[kNode] =
                            newTag().apply { state.updatedRoot().serialize(this) }
                        stack.itemMeta = meta
                    },
                    onClean = {
                        // write directly to stack tag; avoid itemMeta write
                        state.updatedRoot().serialize(tag)
                    }
                )
            }
        }
    }
}
