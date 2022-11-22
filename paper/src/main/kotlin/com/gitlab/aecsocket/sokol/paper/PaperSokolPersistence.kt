package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import com.gitlab.aecsocket.sokol.paper.component.ContainerMap
import com.gitlab.aecsocket.sokol.paper.component.IsChild
import net.kyori.adventure.key.Key
import net.minecraft.nbt.CompoundTag
import org.bukkit.craftbukkit.v1_19_R1.persistence.CraftPersistentDataContainer
import org.bukkit.persistence.PersistentDataContainer

private const val MAP_CHILDREN = "map_children"
const val SLOT_CHILD_KEY = "_"

class PaperSokolPersistence internal constructor(
    sokol: Sokol,
) : SokolPersistence(sokol) {
    val entityKey = sokol.key("entity")
    private lateinit var mIsChild: ComponentMapper<IsChild>
    private lateinit var mContainerMap: ComponentMapper<ContainerMap>

    override fun enable() {
        super.enable()
        mIsChild = sokol.engine.mapper()
        mContainerMap = sokol.engine.mapper()

        macro { _, node, _ ->
            /*

                "sokol:container_map": {
                  child: {
                    "sokol:container_map": {
                      _: "entity_profile_id"
                    }
                  }
                }

            to

                map_children: {
                  child: "entity_profile_id"
                }

             */
            val kContainerMap = ContainerMap.Key.asString()
            val nContainerMap = node.node(kContainerMap)
            node.node(MAP_CHILDREN).childrenMap().forEach { (key, child) ->
                nContainerMap.node(key).node(kContainerMap).node(SLOT_CHILD_KEY).mergeFrom(child)
            }
        }
    }

    override fun tagContext(): CompoundNBTTag = PaperCompoundTag(CompoundTag())

    fun getTag(pdc: PersistentDataContainer, key: Key): CompoundNBTTag? {
        return (pdc as CraftPersistentDataContainer).raw[key.toString()]?.let { PaperCompoundTag(it as CompoundTag) }
    }

    fun forceTag(pdc: PersistentDataContainer, key: Key): CompoundNBTTag {
        return PaperCompoundTag(
            (pdc as CraftPersistentDataContainer).raw.computeIfAbsent(key.toString()) { CompoundTag() } as CompoundTag
        )
    }

    fun removeTag(pdc: PersistentDataContainer, key: Key) {
        (pdc as CraftPersistentDataContainer).raw.remove(key.toString())
    }

    fun writeTagTo(tag: CompoundNBTTag, key: Key, pdc: PersistentDataContainer) {
        (pdc as CraftPersistentDataContainer).raw[key.toString()] = (tag as PaperCompoundTag).backing
    }
}

fun PaperSokolPersistence.writeEntityTagTo(entity: SokolEntity, pdc: PersistentDataContainer) {
    val tag = writeEntity(entity)
    writeTagTo(tag, entityKey, pdc)
}
