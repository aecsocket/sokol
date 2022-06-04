package com.github.aecsocket.sokol.paper

import cloud.commandframework.arguments.standard.StringArgument
import cloud.commandframework.context.CommandContext
import com.github.aecsocket.alexandria.core.extension.render
import com.github.aecsocket.alexandria.core.extension.typeToken
import com.github.aecsocket.alexandria.core.keyed.Keyed
import com.github.aecsocket.alexandria.core.keyed.Registry
import com.github.aecsocket.alexandria.paper.extension.withMeta
import com.github.aecsocket.alexandria.paper.plugin.CloudCommand
import com.github.aecsocket.alexandria.paper.plugin.desc
import com.github.aecsocket.glossa.core.Localizable
import com.github.aecsocket.sokol.paper.feature.TestFeature
import net.kyori.adventure.extra.kotlin.join
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.Locale

internal class SokolCommand(plugin: SokolPlugin) : CloudCommand<SokolPlugin>(
    plugin, "sokol",
    { manager, rootName -> manager.commandBuilder(rootName, desc("Core plugin command.")) }
) {
    init {
        val hosts = root
            .literal("hosts", desc("Gets info on the last hosts resolved on the server."))
        manager.command(hosts
            .permission(perm("command", "hosts"))
            .handler { handle(it, ::hosts) })
        manager.command(hosts
            .literal("toggle", desc("Toggle the hosts HUD element."))
            .permission(perm("command", "hosts", "toggle"))
            .senderType(Player::class.java)
            .handler { handle(it, ::hostsToggle) })

        val list = root
            .literal("list", desc("Lists various registered items."))
        manager.command(list
            .literal("features", desc("Lists all registered features."))
            .argument(StringArgument.optional("filter"), desc("Filter for the name or ID, case-insensitive."))
            .permission(perm("command", "list", "features"))
            .handler { handle(it, ::listFeatures) })
        manager.command(list
            .literal("components", desc("Lists all registered components."))
            .argument(StringArgument.optional("filter"), desc("Filter for the name or ID, case-insensitive."))
            .permission(perm("command", "list", "components"))
            .handler { handle(it, ::listComponents) })
        manager.command(list
            .literal("blueprints", desc("Lists all registered blueprints."))
            .argument(StringArgument.optional("filter"), desc("Filter for the name or ID, case-insensitive."))
            .permission(perm("command", "list", "blueprints"))
            .handler { handle(it, ::listBlueprints) })

        val info = root
            .literal("info", desc("Gets detailed info on a specific registered item."))
        manager.command(info
            .literal("component", desc("Gets info on a registered component."))
            .argument(ComponentArgument(plugin, "item", desc("Item to get info for.")))
            .permission(perm("command", "info", "component"))
            .handler { handle(it, ::infoComponent) })
        manager.command(info
            .literal("blueprint", desc("Gets info on a registered blueprint."))
            .argument(BlueprintArgument(plugin, "item", desc("Item to get info for.")))
            .permission(perm("command", "info", "blueprint"))
            .handler { handle(it, ::infoBlueprint) })

        manager.command(root
            .literal("give")
            .handler { ctx ->
                val player = ctx.sender as Player
                val node = PaperDataNode(
                    PaperComponent("some_component", mapOf(
                        TestFeature.ID to TestFeature(plugin).Profile("abc 123")
                    ), emptyMap(), emptyMap(), emptySet()),
                    /*mutableMapOf(
                        TestFeature.ID to TestFeature(plugin).Profile("abc 123").Data(12345)
                    )*/
                )
                val stack = ItemStack(Material.STICK).withMeta {
                    val tag = plugin.persistence.newTag().apply { node.serialize(this) }
                    plugin.persistence.tagToData(tag, persistentDataContainer)
                    plugin.persistence.setTicks(true, persistentDataContainer)
                }
                player.inventory.addItem(stack)
            }
        )
    }

    fun <T : Keyed> byRegistry(registry: Registry<T>, id: String, locale: Locale) = registry[id]
        ?: error { safe(locale, "error.invalid_id") {
            raw("id") { id }
        } }

    fun component(id: String, locale: Locale) = byRegistry(plugin.components, id, locale)

    fun blueprint(id: String, locale: Locale) = byRegistry(plugin.blueprints, id, locale)


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

    fun hostsToggle(ctx: CommandContext<CommandSender>, sender: CommandSender, locale: Locale) {
        plugin.playerData(sender as Player).apply { showHosts = !showHosts }
    }

    fun <T> list(
        ctx: CommandContext<CommandSender>,
        sender: CommandSender,
        locale: Locale,
        registry: Registry<T>
    ) where T : Keyed, T : Localizable<Component> {
        val filter = ctx.getOrDefault<String>("filter", null)

        fun List<Component>.contains(string: String) = any {
            PlainTextComponentSerializer.plainText().serialize(it).contains(string)
        }

        val results = ArrayList<T>()
        registry.forEach { (_, item) ->
            if (filter?.let {
                item.id.contains(filter)
                    || item.localize(plugin.i18n.withLocale(locale)).contains(filter)
            } != false) {
                results.add(item)
            }
        }

        plugin.send(sender) { safe(locale, "command.list") {
            raw("found") { results.size }
            raw("total") { registry.size }
            list("entries") { results.forEach { item ->
                map {
                    tl("name") { item }
                    raw("id") { item.id }
                }
            } }
        } }
    }

    fun listFeatures(ctx: CommandContext<CommandSender>, sender: CommandSender, locale: Locale) {
        list(ctx, sender, locale, plugin.features)
    }

    fun listComponents(ctx: CommandContext<CommandSender>, sender: CommandSender, locale: Locale) {
        list(ctx, sender, locale, plugin.components)
    }

    fun listBlueprints(ctx: CommandContext<CommandSender>, sender: CommandSender, locale: Locale) {
        list(ctx, sender, locale, plugin.blueprints)
    }


    fun infoComponent(ctx: CommandContext<CommandSender>, sender: CommandSender, locale: Locale) {
        val component = ctx.get<PaperComponent>("item")
        plugin.send(sender) { safe(locale, "command.info.component.message") {
            tl("name") { component }
            raw("id") { component.id }
            list("tags") { component.tags.forEach {
                raw(it)
            } }
            raw("slots_count") { component.slots.size }
            list("slots") { component.slots.forEach { (key, slot) ->
                val hover = text("") // todo

                subList(safe(locale, "command.info.component.slot") {
                    tl("name") { slot }
                    raw("key") { key }
                    raw("required") { slot.required.toString() }
                    raw("modifiable") { slot.modifiable.toString() }
                    list("tags") { slot.tags.forEach {
                        raw(it)
                    } }
                }.map { it.hoverEvent(hover) })
            } }
            raw("features_count") { component.features.size }
            list("features") { component.features.forEach { (key, feature) ->
                val hover = component.featureConfigs[key]!!.render()
                    .join(JoinConfiguration.newlines())

                subList(safe(locale, "command.info.component.feature") {
                    tl("name") { feature.type }
                    raw("id") { key }
                }.map { it.hoverEvent(hover) })
            } }
        } }
    }

    fun infoTree(sender: CommandSender, locale: Locale, node: PaperDataNode): List<Component> {
        val res = ArrayList<Component>()

        res.addAll(plugin.i18n.safe(locale, "command.tree.header") {
            tl("name") { node.component }
            raw("id") { node.component.id }
        })

        fun visitSlot(slot: PaperSlot, node: PaperDataNode?, depth: Int) {
            res.addAll(plugin.i18n.safe(locale, "command.tree.child") {
                raw("indent") { "  ".repeat(depth) }
                tl("slot") { slot }
                subList("value") {
                    node?.let {
                        node.component.localize(this)
                    } ?: safe("command.tree.empty_slot")
                }
            })

            node?.let {
                node.component.slots.forEach { (key, child) ->
                    visitSlot(child, node.node(key), depth + 1)
                }
            }
        }

        node.component.slots.forEach { (key, slot) ->
            visitSlot(slot, node.node(key), 0)
        }

        return res
    }

    fun infoBlueprint(ctx: CommandContext<CommandSender>, sender: CommandSender, locale: Locale) {
        val blueprint = ctx.get<PaperBlueprint>("item")
        plugin.send(sender) { safe(locale, "command.info.blueprint") {
            tl("name") { blueprint }
            raw("id") { blueprint.id }
            subList("tree") { infoTree(sender, locale, blueprint.createNode()) }
        } }
    }
}
