package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.CompoundNBTTag
import com.gitlab.aecsocket.sokol.core.SokolBlueprint
import com.gitlab.aecsocket.sokol.core.SokolComponent
import com.gitlab.aecsocket.sokol.core.SokolEngine
import net.kyori.adventure.key.InvalidKeyException
import net.kyori.adventure.key.Key
import net.minecraft.nbt.CompoundTag
import org.bukkit.craftbukkit.v1_19_R1.persistence.CraftPersistentDataContainer
import org.bukkit.persistence.PersistentDataContainer

class SokolPersistence internal constructor(
    private val sokol: Sokol,
) {
    val entityKey = sokol.key("entity")

    fun newTag(): CompoundNBTTag.Mutable = PaperCompoundTag(CompoundTag())

    fun getTag(pdc: PersistentDataContainer, key: Key): CompoundNBTTag.Mutable? {
        return (pdc as CraftPersistentDataContainer).raw[key.toString()]?.let { PaperCompoundTag(it as CompoundTag) }
    }

    fun forceTag(pdc: PersistentDataContainer, key: Key): CompoundNBTTag.Mutable {
        return PaperCompoundTag(
            (pdc as CraftPersistentDataContainer).raw.computeIfAbsent(key.toString()) { CompoundTag() } as CompoundTag
        )
    }

    fun writeTagTo(tag: CompoundNBTTag, key: Key, pdc: PersistentDataContainer) {
        (pdc as CraftPersistentDataContainer).raw[key.toString()] = (tag as PaperCompoundTag).backing
    }

    fun readBlueprint(tag: CompoundNBTTag): SokolBlueprint {
        val components = tag.map { (rawKey, child) ->
            child as? CompoundNBTTag
                ?: throw IllegalArgumentException("Component for key '$rawKey' is not compound tag")

            val key = try {
                Key.key(rawKey)
            } catch (ex: InvalidKeyException) {
                throw IllegalArgumentException("Invalid component key '$rawKey'", ex)
            }

            val type = sokol.componentType(key)
                ?: throw IllegalArgumentException("Invalid component key '$key'")

            type.read(child)
        }
        return SokolBlueprint(components)
    }

    private fun SokolComponent.writeInto(tag: CompoundNBTTag.Mutable) {
        if (this is PersistentComponent) {
            tag.set(key.toString()) { ofCompound().apply { write(this) } }
        }
    }

    fun writeBlueprint(blueprint: SokolBlueprint, tag: CompoundNBTTag.Mutable) {
        tag.clear()
        blueprint.components.forEach { it.writeInto(tag) }
    }

    fun writeEntity(space: SokolEngine.Space, entity: Int, tag: CompoundNBTTag.Mutable) {
        tag.clear()
        space.getComponents(entity).forEach { it.writeInto(tag) }
    }
}

fun CompoundNBTTag.Mutable.forComponent(component: PersistentComponent): CompoundNBTTag.Mutable {
    val key = component.key.asString()
    return (get(key) as? CompoundNBTTag.Mutable) ?: PaperCompoundTag(CompoundTag()).also { set(key, it) }
}
