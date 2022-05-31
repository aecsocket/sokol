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
            .literal("hosts", desc("Gets info on the last hosts resolved on the server."))
            .permission(perm("command", "hosts"))
            .handler { handle(it, ::hosts) }
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
                    plugin.persistence.setTick(meta.persistentDataContainer)
                }
                player.inventory.addItem(item)
            }
        )
    }

    fun hosts(ctx: CommandContext<CommandSender>, sender: CommandSender, locale: Locale) {
        val hosts = plugin.lastHosts
        val possible = hosts.values.sumOf { it.possible }
        val marked = hosts.values.sumOf { it.marked }
        plugin.send(sender) { safe(locale, "command.hosts") {
            raw("possible") { possible }
            raw("marked") { marked }
            raw("percent") { marked.toDouble() / possible }
            list("types") { hosts.map { (type, data) ->
                map {
                    raw("name") { type }
                    raw("possible") { data.possible }
                    raw("marked") { data.marked }
                    raw("percent") { data.marked.toDouble() / data.possible }
                }
            } }
        } }
    }
}
