package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.*
import net.kyori.adventure.key.Key
import net.minecraft.nbt.CompoundTag
import org.bukkit.craftbukkit.v1_19_R1.persistence.CraftPersistentDataContainer
import org.bukkit.persistence.PersistentDataContainer

class PaperSokolPersistence internal constructor(
    sokol: Sokol,
) : SokolPersistence(sokol) {
    val entityKey = sokol.key("entity")

    override fun newTag(): CompoundNBTTag.Mutable = PaperCompoundTag(CompoundTag())

    fun getTag(pdc: PersistentDataContainer, key: Key): CompoundNBTTag.Mutable? {
        return (pdc as CraftPersistentDataContainer).raw[key.toString()]?.let { PaperCompoundTag(it as CompoundTag) }
    }

    fun forceTag(pdc: PersistentDataContainer, key: Key): CompoundNBTTag.Mutable {
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
