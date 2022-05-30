package com.github.aecsocket.sokol.paper

import cloud.commandframework.context.CommandContext
import com.github.aecsocket.alexandria.paper.extension.withMeta
import com.github.aecsocket.alexandria.paper.plugin.CloudCommand
import com.github.aecsocket.alexandria.paper.plugin.desc
import com.github.aecsocket.sokol.paper.feature.TestFeature
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import java.util.Locale

internal class SokolCommand(plugin: SokolPlugin) : CloudCommand<SokolPlugin>(
    plugin, "sokol",
    { manager, rootName -> manager.commandBuilder(rootName, desc("Core plugin command.")) }
) {
    init {
        manager.command(root
            .literal("representatives", desc("Gets info on the last representatives found."))
            .permission(perm("command", "representatives"))
            .handler { handle(it, ::representatives) }
        )
        manager.command(root
            .literal("give")
            .handler { ctx ->
                val player = ctx.sender as Player
                val node = PaperDataNode(
                    PaperComponent("some_component", mapOf(
                        TestFeature.ID to TestFeature(plugin).Profile("abc 123")
                    ), emptyMap()),
                    /*mutableMapOf(
                        TestFeature.ID to TestFeature(plugin).Profile("abc 123").Data(12345)
                    )*/
                )
                val item = ItemStack(Material.STICK).withMeta<ItemMeta> { meta ->
                    plugin.persistence.set(meta.persistentDataContainer, node)
                    plugin.persistence.setOnTick(meta.persistentDataContainer)
                }
                player.inventory.addItem(item)
            }
        )
    }

    fun representatives(ctx: CommandContext<CommandSender>, sender: CommandSender, locale: Locale) {
        val reprs = plugin.lastRepresentatives
        val possible = reprs.values.sumOf { (ps, _) -> ps }
        val actual = reprs.values.sumOf { (_, ac) -> ac }
        plugin.send(sender) { safe(locale, "command.representatives") {
            raw("possible") { possible }
            raw("actual") { actual }
            raw("percent") { 0.5 }
            list("types") { reprs.map { (type, data) ->
                map {
                    raw("name") { type }
                    raw("possible") { data.first }
                    raw("actual") { data.second }
                    raw("percent") { data.second.toDouble() / data.first }
                }
            } }
        } }
    }
}
