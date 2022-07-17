package com.gitlab.aecsocket.sokol.paper.extension

import com.gitlab.aecsocket.alexandria.paper.extension.bukkit
import com.gitlab.aecsocket.alexandria.paper.extension.withMeta
import com.gitlab.aecsocket.sokol.core.ItemDescriptor
import org.bukkit.Registry
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable

fun ItemDescriptor.asStack(): ItemStack {
    val stack = ItemStack(Registry.MATERIAL.get(key.bukkit())
        ?: throw IllegalStateException("No material with key '$key'"))
    stack.withMeta {
        flags.forEach { addItemFlags(ItemFlag.valueOf(it)) }
        setCustomModelData(modelData)
        isUnbreakable = unbreakable
        if (this is Damageable) {
            damage = this@asStack.damage
        }
    }
    return stack
}
