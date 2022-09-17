package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.paper.extension.key
import com.gitlab.aecsocket.sokol.core.CompoundNBTTag
import com.gitlab.aecsocket.sokol.core.SokolEntity
import com.gitlab.aecsocket.sokol.core.SokolHost
import net.kyori.adventure.key.InvalidKeyException
import net.kyori.adventure.key.Key
import net.minecraft.nbt.CompoundTag
import org.bukkit.craftbukkit.v1_19_R1.persistence.CraftPersistentDataContainer
import org.bukkit.persistence.PersistentDataContainer

private const val COMPONENTS = "components"

class SokolPersistence internal constructor(
    private val sokol: Sokol,
) {
    val entityKey = sokol.key("entity")

    fun newTag(): CompoundNBTTag.Mutable = PaperCompoundTag(CompoundTag())

    fun writeTagTo(tag: CompoundNBTTag, key: Key, pdc: PersistentDataContainer) {
        (pdc as CraftPersistentDataContainer).raw[key.toString()] = (tag as PaperCompoundTag).backing
    }

    fun readEntity(tag: CompoundNBTTag, host: SokolHost): SokolEntity {
        val entity = PaperEntity(host)
        tag.forEach { (rawKey, child) ->
            child as? CompoundNBTTag
                ?: throw IllegalArgumentException("Component for key '$rawKey' is not compound tag")

            val key = try {
                Key.key(rawKey)
            } catch (ex: InvalidKeyException) {
                throw IllegalArgumentException("Invalid component key '$rawKey'", ex)
            }
            val type = sokol.componentTypes[key]
                ?: throw IllegalArgumentException("Invalid component key '$key'")
            val component = type.deserialize(child)
            entity.addComponent(component)
        }
        return entity
    }

    fun writeEntity(entity: SokolEntity, tag: CompoundNBTTag.Mutable) {
        tag.clear()
        entity.components.forEach { (key, component) ->
            tag.set(key.toString()) { ofCompound().apply { component.serialize(this) } }
        }
    }
}
