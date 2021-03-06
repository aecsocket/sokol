package com.gitlab.aecsocket.sokol.paper

import cloud.commandframework.arguments.standard.BooleanArgument
import cloud.commandframework.arguments.standard.EnumArgument
import cloud.commandframework.arguments.standard.IntegerArgument
import cloud.commandframework.arguments.standard.StringArgument
import cloud.commandframework.bukkit.arguments.selector.MultiplePlayerSelector
import cloud.commandframework.bukkit.parsers.location.LocationArgument
import cloud.commandframework.bukkit.parsers.selector.MultiplePlayerSelectorArgument
import cloud.commandframework.context.CommandContext
import com.gitlab.aecsocket.alexandria.core.extension.*
import com.gitlab.aecsocket.alexandria.core.keyed.Keyed
import com.gitlab.aecsocket.alexandria.core.keyed.Registry
import com.gitlab.aecsocket.alexandria.core.physics.Transform
import com.gitlab.aecsocket.alexandria.core.physics.Vector3
import com.gitlab.aecsocket.alexandria.paper.extension.vector
import com.gitlab.aecsocket.alexandria.paper.plugin.CloudCommand
import com.gitlab.aecsocket.alexandria.paper.plugin.desc
import com.gitlab.aecsocket.glossa.core.Localizable
import com.gitlab.aecsocket.sokol.core.BlueprintParser
import com.gitlab.aecsocket.sokol.core.ComponentParser
import com.gitlab.aecsocket.sokol.core.NodeParser
import com.gitlab.aecsocket.sokol.core.feature.HostCreationException
import net.kyori.adventure.extra.kotlin.join
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*

private val Boolean.tlKey get() = if (this) "enabled" else "disabled"

internal class SokolCommand(plugin: Sokol) : CloudCommand<Sokol>(
    plugin, "sokol",
    { manager, rootName -> manager.commandBuilder(rootName, desc("Core plugin command.")) }
) {
    init {
        captions?.apply {
            registerMessageFactory(ComponentParser.ARGUMENT_PARSE_FAILURE_COMPONENT, captionLocalizer)
            registerMessageFactory(BlueprintParser.ARGUMENT_PARSE_FAILURE_BLUEPRINT, captionLocalizer)
            registerMessageFactory(NodeParser.ARGUMENT_PARSE_FAILURE_DATA_NODE_REGISTRY, captionLocalizer)
            registerMessageFactory(NodeParser.ARGUMENT_PARSE_FAILURE_DATA_NODE_MALFORMED, captionLocalizer)
        }

        val hosts = root
            .literal("hosts", desc("Gets info on the last hosts resolved on the server."))
        manager.command(hosts
            .permission(perm("hosts"))
            .handler { handle(it, ::hosts) })
        manager.command(hosts
            .literal("toggle", desc("Toggle the hosts HUD element."))
            .permission(perm("hosts", "toggle"))
            .senderType(Player::class.java)
            .handler { handle(it, ::hostsToggle) })

        val list = root
            .literal("list", desc("Lists various registered items."))
        manager.command(list
            .literal("features", desc("Lists all registered features."))
            .argument(StringArgument.optional("filter"), desc("Filter for the name or ID, case-insensitive."))
            .permission(perm("list", "features"))
            .handler { handle(it, ::listFeatures) })
        manager.command(list
            .literal("components", desc("Lists all registered components."))
            .argument(StringArgument.optional("filter"), desc("Filter for the name or ID, case-insensitive."))
            .permission(perm("list", "components"))
            .handler { handle(it, ::listComponents) })
        manager.command(list
            .literal("blueprints", desc("Lists all registered blueprints."))
            .argument(StringArgument.optional("filter"), desc("Filter for the name or ID, case-insensitive."))
            .permission(perm("list", "blueprints"))
            .handler { handle(it, ::listBlueprints) })

        val info = root
            .literal("info", desc("Gets detailed info on a specific registered item."))
        manager.command(info
            .literal("component", desc("Gets info on a registered component."))
            .argument(PaperComponentArgument(plugin, "item", desc("Item to get info for.")))
            .permission(perm("info", "component"))
            .handler { handle(it, ::infoComponent) })
        manager.command(info
            .literal("blueprint", desc("Gets info on a registered blueprint."))
            .argument(PaperBlueprintArgument(plugin, "item", desc("Item to get info for.")))
            .permission(perm("info", "blueprint"))
            .handler { handle(it, ::infoBlueprint) })

        val render = root
            .literal("render", desc("Options for rendered items,"))
        manager.command(render
            .literal("show-shapes", desc("Toggles showing shape and point definitions on the selected render."))
            .argument(EnumArgument.optional(DefaultNodeRenders.ShowShapes::class.java, "state"), desc("For what objects to show shapes of."))
            .permission(perm("render", "show-shapes"))
            .senderType(Player::class.java)
            .handler { handle(it, ::renderShowShapes) })
        manager.command(render
            .literal("bypass-options", desc("Toggles bypassing render options, allowing you to modify a render at will."))
            .argument(BooleanArgument.optional("state"), desc("Feature state."))
            .permission(perm("render", "bypass-options"))
            .senderType(Player::class.java)
            .handler { handle(it, ::renderBypassOptions) })
        manager.command(render
            .literal("spawn", desc("Spawns a node render."))
            .argument(PaperNodeArgument(plugin, "node", desc("Node to spawn the render of.")))
            .flag(manager.flagBuilder("pos")
                .withDescription(desc("Position of the render."))
                .withArgument(LocationArgument.of<CommandSender>("pos"))
                .build())
            .flag(manager.flagBuilder("rot")
                .withDescription(desc("Rotation of the render."))
                .withArgument(LocationArgument.of<CommandSender>("pos"))
                .build())
            .flag(manager.flagBuilder("selectable")
                .withDescription(desc("If the render can be highlighted."))
                .withAliases("s")
                .build())
            .flag(manager.flagBuilder("movable")
                .withDescription(desc("If the render can be dragged and moved."))
                .withAliases("m")
                .build())
            .flag(manager.flagBuilder("rotatable")
                .withDescription(desc("If the render can be rotated."))
                .withAliases("r")
                .build())
            .flag(manager.flagBuilder("grabbable")
                .withDescription(desc("If the render can be grabbed and removed."))
                .withAliases("g")
                .build())
            .flag(manager.flagBuilder("modifiable")
                .withDescription(desc("If the render can have parts added and removed."))
                .withAliases("d")
                .build())
            .flag(manager.flagBuilder("unrestricted")
                .withDescription(desc("If non-modifiable slots can also be added and removed to/from."))
                .withAliases("u")
                .build())
            .permission(perm("render", "spawn"))
            .senderType(Player::class.java)
            .handler { handle(it, ::renderSpawn) })

        manager.command(root
            .literal("give", desc("Gives a specified item-representable node tree to a player."))
            .argument(MultiplePlayerSelectorArgument.of("targets"), desc("Players to give to."))
            .argument(PaperNodeArgument(plugin, "item", desc("Node to give.")))
            .argument(IntegerArgument.optional("amount"), desc("Amount of the item to give."))
            .permission(perm("give"))
            .handler { handle(it, ::give) })
        manager.command(root
            .literal("build", desc("Builds and gives a specified item-representable blueprint to a player."))
            .argument(MultiplePlayerSelectorArgument.of("targets"), desc("Players to give to."))
            .argument(PaperBlueprintArgument(plugin, "item", desc("Blueprint to give.")))
            .argument(IntegerArgument.optional("amount"), desc("Amount of the item to give."))
            .permission(perm("build"))
            .handler { handle(it, ::build) })
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
        plugin.playerState(sender as Player).apply { showHosts = !showHosts }
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
            raw("qt_slots") { component.slots.size }
            list("slots") { component.slots.forEach { (key, slot) ->
                val hover = text("") // todo

                subList(safe("command.info.component.slot") {
                    tl("name") { slot }
                    raw("key") { key }
                    raw("required") { slot.required.toString() }
                }.map { it.hoverEvent(hover) })
            } }
            raw("qt_features") { component.features.size }
            list("features") { component.features.forEach { (key, feature) ->
                val hover = component.featureConfigs[key]!!.render()
                    .join(JoinConfiguration.newlines())

                subList(safe("command.info.component.feature") {
                    tl("name") { feature.type }
                    raw("id") { key }
                }.map { it.hoverEvent(hover) })
            } }
            raw("qt_stats") { component.stats.size }
            list("stats") { component.stats.forEach { stats ->
                // todo hover

                subList(safe("command.info.component.stats") {
                    raw("priority") { stats.priority }
                    raw("reversed") { stats.reversed.toString() }
                    raw("qt_entries") { stats.stats.entries.size }
                })
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

    fun renderShowShapes(ctx: CommandContext<CommandSender>, sender: CommandSender, locale: Locale) {
        val pState = plugin.playerState(sender as Player)
        val state = ctx.get("state") { DefaultNodeRenders.ShowShapes.NONE }

        pState.renders.showShapes = state
        plugin.send(sender) { safe(locale, "command.render.show_shapes.${state.key}") }
    }

    fun renderBypassOptions(ctx: CommandContext<CommandSender>, sender: CommandSender, locale: Locale) {
        val pState = plugin.playerState(sender as Player)
        val state = ctx.get("state") { !pState.renders.bypassOptions }

        pState.renders.bypassOptions = state
        plugin.send(sender) { safe(locale, "command.render.bypass_options.${state.tlKey}") }
    }

    fun renderSpawn(ctx: CommandContext<CommandSender>, sender: CommandSender, locale: Locale) {
        sender as Player
        val node = ctx.get<PaperDataNode>("node")
        val pos = (ctx.flag<Location>("pos") ?: sender.location).vector()
        val rot = ctx.flag<Location>("rot")?.vector() ?: Vector3.Zero
        val options = NodeRenders.Options(
            selectable = ctx.flagged("selectable"),
            movable = ctx.flagged("movable"),
            rotatable = ctx.flagged("rotatable"),
            grabbable = ctx.flagged("grabbable"),
            modifiable = ctx.flagged("modifiable"),
            unrestricted = ctx.flagged("unrestricted"),
        )

        plugin.renders.create(node, sender.world, Transform(
            rot = rot.radians.quaternion(EulerOrder.XYZ),
            tl = pos,
        ), options)
    }

    fun give(
        ctx: CommandContext<CommandSender>,
        sender: CommandSender,
        locale: Locale,
        node: PaperDataNode
    ) {
        val targets = ctx.get<MultiplePlayerSelector?>("targets") { null }.orSender("targets", sender, locale)
        val amount = ctx.get("amount") { 1 }

        val state = paperStateOf(node)
        val items = targets.map { target ->
            val host = hostOf(target)
            target to try {
                plugin.persistence.forceStateToStack(holderBy(host), state)
            } catch (ex: HostCreationException) {
                error(ex) { safe(locale, "error.creating_host") }
            }
        }

        items.forEach { (target, item) ->
            repeat(amount) { target.inventory.addItem(item) }
        }

        val itemName = node.component.localize(plugin.i18n.withLocale(locale)).join()
        plugin.send(sender) {
            if (targets.size == 1) {
                safe(locale, "command.give.one") {
                    sub("item") { itemName }
                    sub("target") { targets.first().displayName() }
                    raw("amount") { amount }
                }
            } else {
                safe(locale, "command.give.other") {
                    sub("item") { itemName }
                    raw("qt_targets") { targets.size }
                    raw("amount") { amount }
                }
            }
        }
    }

    fun give(ctx: CommandContext<CommandSender>, sender: CommandSender, locale: Locale) {
        give(ctx, sender, locale, ctx.get<PaperDataNode>("item"))
    }

    fun build(ctx: CommandContext<CommandSender>, sender: CommandSender, locale: Locale) {
        give(ctx, sender, locale, ctx.get<PaperBlueprint>("item").createNode())
    }
}
