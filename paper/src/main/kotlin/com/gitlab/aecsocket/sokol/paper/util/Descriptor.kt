package com.gitlab.aecsocket.sokol.paper.util

import com.gitlab.aecsocket.alexandria.paper.extension.withMeta
import org.bukkit.Material
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Required

@ConfigSerializable
data class ItemDescriptor(
    @Required val material: Material,
    val damage: Int = 0,
    val modelData: Int = 0,
    val unbreakable: Boolean = false,
    val flags: List<String> = emptyList(),
) {
    fun create(): ItemStack {
        return ItemStack(material).withMeta { meta ->
            (meta as? Damageable)?.damage = damage
            meta.setCustomModelData(modelData)
            meta.isUnbreakable = unbreakable
            meta.addItemFlags(*flags.map { ItemFlag.valueOf(it) }.toTypedArray())
        }
    }
}
