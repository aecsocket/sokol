package com.github.aecsocket.sokol.paper

import com.github.aecsocket.alexandria.core.LogLevel
import com.github.aecsocket.alexandria.core.keyed.by
import com.github.aecsocket.alexandria.core.physics.Quaternion
import com.github.aecsocket.alexandria.core.physics.Transform
import com.github.aecsocket.alexandria.paper.datatype.QuaternionDataType
import com.github.aecsocket.alexandria.paper.extension.key
import com.github.aecsocket.alexandria.paper.extension.location
import com.github.aecsocket.alexandria.paper.extension.withMeta
import com.github.aecsocket.sokol.core.NodePath
import com.github.aecsocket.sokol.core.SokolPersistence
import com.github.aecsocket.sokol.core.emptyNodePath
import com.github.aecsocket.sokol.core.feature.ItemHostFeature
import com.github.aecsocket.sokol.core.keyOf
import com.github.aecsocket.sokol.core.nbt.BinaryTag
import com.github.aecsocket.sokol.core.nbt.CompoundBinaryTag
import com.github.aecsocket.sokol.core.nbt.TagSerializationException
import com.github.aecsocket.sokol.paper.extension.asStack
import net.kyori.adventure.key.Key
import net.minecraft.nbt.ByteTag
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NumericTag
import org.bukkit.World
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftAreaEffectCloud
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack
import org.bukkit.craftbukkit.v1_18_R2.persistence.CraftPersistentDataContainer
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
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

class NodeItemCreationException(message: String? = null, cause: Throwable? = null) : RuntimeException(message, cause)

class PaperPersistence internal constructor(
    private val plugin: SokolPlugin
) : SokolPersistence<PaperDataNode> {
    val kRender = plugin.key("render")
    val kRot = plugin.key("rot")
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

    fun stateToStack(state: PaperTreeState): ItemStack {
        val node = state.root
        val itemHost = state.nodeStates[node]?.by<ItemHostFeature.State<*, *, *>>(ItemHostFeature)
            ?: throw NodeItemCreationException("No feature '${ItemHostFeature.id}' to create item from")

        return try {
            itemHost.itemDescriptor(state).asStack().withMeta {
                nodeInto(node, forceNodeTagOf(persistentDataContainer))
            }
        } catch (ex: Exception) {
            throw NodeItemCreationException(cause = ex)
        }
    }

    fun nodeToStack(node: PaperDataNode): ItemStack {
        val state = paperStateOf(node)
        return stateToStack(state)
    }

    // Ticks

    fun setTicks(value: Boolean, pdc: PersistentDataContainer) {
        (pdc as CraftPersistentDataContainer).raw[sTick] = ByteTag.valueOf(value)
    }

    fun ticks(pdc: PersistentDataContainer): Boolean {
        return (pdc as CraftPersistentDataContainer).raw[sTick]?.let {
            it is NumericTag && it.asByte != (0).toByte()
        } == true
    }

    // Render

    fun setRender(node: PaperDataNode, rot: Quaternion, pdc: PersistentDataContainer) {
        pdc.set(kRender, PersistentDataType.TAG_CONTAINER, pdc.adapterContext.newPersistentDataContainer().apply {
            set(kRot, QuaternionDataType, rot)
            nodeInto(node, forceNodeTagOf(this))
        })
    }

    fun getRender(entity: Entity): NodeRender? {
        entity.persistentDataContainer.get(kRender, PersistentDataType.TAG_CONTAINER)?.let { pdc ->
            nodeTagOf(pdc)?.let { nodeOf(it) }?.let { node ->
                val rot = pdc.getOrDefault(kRot, QuaternionDataType, Quaternion.Identity)
                return plugin.renders.create(entity, node, rot)
            } ?: run {
                plugin.log.line(LogLevel.Warning) { "Found render on $entity with no key $kNode - removing" }
                entity.remove()
            }
        }
        return null
    }

    fun spawnRender(node: PaperDataNode, world: World, transform: Transform) {
        world.spawnEntity(transform.tl.location(world), EntityType.AREA_EFFECT_CLOUD, CreatureSpawnEvent.SpawnReason.CUSTOM) { entity ->
            setRender(node, transform.rot, entity.persistentDataContainer)
            entity as CraftAreaEffectCloud
            entity.handle.apply {
                // avoid Bukkit's limits - I know what I'm doing
                tickCount = Int.MIN_VALUE
                duration = -1
                waitTime = Int.MIN_VALUE
            }
        }
    }
}
