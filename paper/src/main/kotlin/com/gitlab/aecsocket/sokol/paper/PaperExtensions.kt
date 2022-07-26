package com.gitlab.aecsocket.sokol.paper

import com.gitlab.aecsocket.alexandria.core.ComponentTableRenderer
import com.gitlab.aecsocket.alexandria.paper.AlexandriaAPI
import com.gitlab.aecsocket.alexandria.paper.extension.bukkit
import com.gitlab.aecsocket.alexandria.paper.extension.withMeta
import com.gitlab.aecsocket.sokol.core.ItemDescriptor
import com.gitlab.aecsocket.sokol.core.util.TableFormat
import net.kyori.adventure.extra.kotlin.join
import net.kyori.adventure.text.Component.empty
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

fun TableFormat.buildRenderer(plugin: Sokol): ComponentTableRenderer {
    val colSeparator = colSeparatorKey?.let { key -> plugin.i18n.make(key)?.join() } ?: empty()
    return AlexandriaAPI.ComponentTableRenderer(
        { align[it] }, { justify[it] },
        colSeparator,
        { widths -> rowSeparatorKey?.let { key -> plugin.i18n.make(key) {
            list("padding") { widths.forEach { raw(AlexandriaAPI.paddingOf(it)) } }
        } } ?: emptyList() }
    )
}
