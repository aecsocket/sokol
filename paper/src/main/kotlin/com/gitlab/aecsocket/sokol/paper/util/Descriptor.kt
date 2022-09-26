package com.gitlab.aecsocket.sokol.paper.util

import com.gitlab.aecsocket.alexandria.paper.extension.withMeta
import com.gitlab.aecsocket.sokol.core.util.ItemDescriptor
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable

fun ItemDescriptor.create(): ItemStack {
    val material = Registry.MATERIAL[NamespacedKey(key.namespace(), key.value())]
        ?: throw IllegalStateException("Invalid material '$key'")
    return ItemStack(material).withMeta { meta ->
        (meta as? Damageable)?.damage = damage
        meta.setCustomModelData(modelData)
        meta.isUnbreakable = unbreakable
        meta.addItemFlags(*flags.map { ItemFlag.valueOf(it) }.toTypedArray())
    }
}
