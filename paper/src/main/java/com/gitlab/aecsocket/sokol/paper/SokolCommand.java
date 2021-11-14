package com.gitlab.aecsocket.sokol.paper;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.bukkit.parsers.selector.MultiplePlayerSelectorArgument;
import cloud.commandframework.context.CommandContext;
import com.gitlab.aecsocket.minecommons.core.Components;
import com.gitlab.aecsocket.minecommons.core.Text;
import com.gitlab.aecsocket.minecommons.paper.plugin.BaseCommand;
import com.gitlab.aecsocket.sokol.core.node.ItemCreationException;
import com.gitlab.aecsocket.sokol.core.stat.Stat;
import com.gitlab.aecsocket.sokol.core.stat.StatIntermediate;
import com.gitlab.aecsocket.sokol.core.stat.StatMap;
import com.gitlab.aecsocket.sokol.paper.command.BlueprintArgument;
import com.gitlab.aecsocket.sokol.paper.command.ComponentArgument;
import com.gitlab.aecsocket.sokol.paper.command.NodeArgument;
import com.gitlab.aecsocket.sokol.paper.impl.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static com.gitlab.aecsocket.sokol.paper.SokolPlugin.PERMISSION_PREFIX;
import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.JoinConfiguration.*;

/* package */ final class SokolCommand extends BaseCommand<SokolPlugin> {
    private static final NamedTextColor separator = NamedTextColor.GRAY;

    SokolCommand(SokolPlugin plugin) throws Exception {
        super(plugin, "sokol",
                (mgr, root) -> mgr.commandBuilder(root, ArgumentDescription.of("Plugin main command.")));

        manager.command(root
                .literal("tree", ArgumentDescription.of("Shows a node tree of the held item."))
                .senderType(Player.class)
                .permission(PERMISSION_PREFIX + ".command.tree")
                .handler(c -> handle(c, this::tree)));
        manager.command(root
                .literal("component", ArgumentDescription.of("Outputs detailed information on a registered component."))
                .argument(ComponentArgument.of(plugin, "component"), ArgumentDescription.of("The component to list information on."))
                .permission(PERMISSION_PREFIX + ".command.component")
                .handler(c -> handle(c, this::component)));
        manager.command(root
                .literal("blueprint", ArgumentDescription.of("Outputs detailed information on a registered blueprint."))
                .argument(BlueprintArgument.of(plugin, "blueprint"), ArgumentDescription.of("The blueprint to list information on."))
                .permission(PERMISSION_PREFIX + ".command.blueprint")
                .handler(c -> handle(c, this::blueprint)));
        manager.command(root
                .literal("give", ArgumentDescription.of("Gives a component/node tree to specified players."))
                .argument(MultiplePlayerSelectorArgument.of("targets"), ArgumentDescription.of("The players to give the item to."))
                .argument(NodeArgument.of(plugin, "node"), ArgumentDescription.of("The component/node tree to give."))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1).asOptional(), ArgumentDescription.of("The amount of the item to give."))
                .permission(PERMISSION_PREFIX + ".command.give")
                .handler(c -> handle(c, this::give)));
        manager.command(root
                .literal("build", ArgumentDescription.of("Gives a built blueprint to specified players."))
                .argument(MultiplePlayerSelectorArgument.of("targets"), ArgumentDescription.of("The players to give the item to."))
                .argument(BlueprintArgument.of(plugin, "blueprint"), ArgumentDescription.of("The blueprint to give."))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1).asOptional(), ArgumentDescription.of("The amount of the item to give."))
                .permission(PERMISSION_PREFIX + ".command.build")
                .handler(c -> handle(c, this::build)));
    }

    private List<Component> formatStats(Locale locale, StatMap map) {
        List<Component> lines = new ArrayList<>();
        lc.lines(locale, PREFIX_COMMAND + ".stats.header",
                        "amount", map.size()+"")
                .ifPresent(lines::addAll);
        for (var entry : map.entrySet()) {
            String key = entry.getKey();
            Stat.Node<?> node = entry.getValue();
            List<? extends Stat.Node<?>> chain = node.asList();
            lc.lines(locale, PREFIX_COMMAND + ".stats.entry",
                            "name", node.stat().render(locale, lc),
                            "key", key,
                            "nodes", chain.size() + "",
                            "chain", join(separator(text(", ", separator)),
                                    chain.stream().map(n -> n.value().render(locale, lc)).collect(Collectors.toList())))
                    .ifPresent(lines::addAll);
        }

        return lines;
    }

    private Component treeHover(Locale locale, String command) {
        return lc.lines(locale, PREFIX_COMMAND + ".tree.hover",
                        "command", command)
                .map(m -> join(separator(newline()), m))
                .orElse(empty());
    }

    private void tree0(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, @Nullable Player pSender, Component indent, int depth, PaperNode node) {
        for (var entry : node.nodes().entrySet()) {
            String key = entry.getKey();
            PaperNode child = entry.getValue();
            PaperComponent component = child.value();

            String command = "/" + rootName + " component " + component.id();
            Component hover = treeHover(locale, command);
            ClickEvent click = ClickEvent.runCommand(command);
            lc.lines(locale, PREFIX_COMMAND + ".tree.node",
                            "indent", Components.repeat(indent, depth),
                            "slot", node.value().slot(key).orElseThrow(IllegalStateException::new).render(locale, lc),
                            "key", key,
                            "name", component.render(locale, lc),
                            "id", component.id())
                    .ifPresent(m -> m.forEach(c -> sender.sendMessage(c.hoverEvent(hover).clickEvent(click))));
            tree0(ctx, sender, locale, pSender, indent, depth + 1, child);
        }
    }

    private void tree(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, @Nullable Player pSender, PaperNode node, String type) {
        String command = "/" + rootName + " component " + node.value().id();
        Component hover = treeHover(locale, command);
        ClickEvent click = ClickEvent.runCommand(command);
        lc.lines(locale, PREFIX_COMMAND + ".tree." + type,
                        "type", node.value().getClass().getSimpleName(),
                        "name", node.value().render(locale, lc),
                        "id", node.value().id())
                .ifPresent(m -> m.forEach(c -> sender.sendMessage(c.hoverEvent(hover).clickEvent(click))));
        tree0(ctx, sender, locale, pSender, lc.safe(locale, PREFIX_COMMAND + ".tree.indent"), 0, node);
    }

    private void tree(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, @Nullable Player pSender) {
        if (pSender == null)
            return;
        ItemStack item = pSender.getInventory().getItemInMainHand();
        if (!item.hasItemMeta())
            throw error("item_no_meta");
        PaperNode node = plugin.persistence().safeLoad(item.getItemMeta().getPersistentDataContainer())
                .orElseThrow(() -> error("item_not_tree"))
                .initialize();
        tree(ctx, sender, locale, pSender, node, "header");

        // todo build it so it works
        node.treeData().ifPresent(treeData -> {
            StatMap stats = treeData.stats();
            Component hover = join(separator(newline()), formatStats(locale, stats));
            lc.lines(locale, PREFIX_COMMAND + ".tree.stats",
                    "amount", ""+stats.size())
                    .ifPresent(m -> m.forEach(c -> sender.sendMessage(c.hoverEvent(hover))));
        });
    }

    private void component(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, @Nullable Player pSender) {
        PaperComponent component = ctx.get("component");

        send(sender, locale, "tree.header",
                "type", component.getClass().getSimpleName(),
                "name", component.render(locale, lc),
                "id", component.id());

        component.renderDescription(locale, lc).ifPresent(desc -> {
            for (var line : desc)
                send(sender, locale, "component.description",
                        "line", line);
        });
        send(sender, locale, "component.tags",
                "tags", String.join(", ", component.tags()));

        send(sender, locale, "component.slots",
                "amount", component.slots().size()+"");
        for (var entry : component.slots().entrySet()) {
            String key = entry.getKey();
            PaperSlot slot = entry.getValue();

            Component hover = slot.rule().render(locale, lc);
            lc.lines(locale, PREFIX_COMMAND + ".component.slot",
                    "name", slot.render(locale, lc),
                    "key", key,
                    "tags", String.join(", ", slot.tags()),
                    "offset", slot.offset()+"")
                    .ifPresent(m -> m.forEach(c -> sender.sendMessage(c.hoverEvent(hover))));
        }

        send(sender, locale, "component.features",
                "amount", component.features().size()+"");
        for (var entry : component.features().entrySet()) {
            String id = entry.getKey();
            PaperFeature<?> feature = entry.getValue();

            Component hover = feature.renderConfig(locale, lc)
                    .map(cfg -> join(separator(newline()), cfg))
                    .orElse(empty());
            lc.lines(locale, PREFIX_COMMAND + ".component.feature",
                    "name", feature.render(locale, lc),
                    "id", id,
                    "description", feature.renderDescription(locale, lc))
                    .ifPresent(m -> m.forEach(c -> sender.sendMessage(c.hoverEvent(hover))));
        }

        List<StatIntermediate.MapData> stats = component.stats().join();
        send(sender, locale, "component.stats",
                "amount", stats.size()+"");
        for (var data : stats) {
            StatMap map = data.stats();
            List<Component> lines = formatStats(locale, data.stats());
            Component hover = join(separator(newline()), lines);
            lc.lines(locale, PREFIX_COMMAND + ".component.stat_map",
                    "priority", data.priority().toString(),
                    "amount", map.size()+"",
                    "rule", data.rule().render(locale, lc))
                    .ifPresent(m -> m.forEach(c -> sender.sendMessage(c.hoverEvent(hover))));
        }
    }

    private void blueprint(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, @Nullable Player pSender) {
        PaperBlueprint blueprint = ctx.get("blueprint");

        send(sender, locale, "tree.header",
                "type", blueprint.getClass().getSimpleName(),
                "name", blueprint.render(locale, lc),
                "id", blueprint.id());
        blueprint.renderDescription(locale, lc).ifPresent(desc -> {
            for (var line : desc)
                send(sender, locale, "tree.description",
                        "line", line);
        });

        PaperNode node = blueprint.node();
        tree(ctx, sender, locale, pSender, node, "root");
    }

    private void give(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, @Nullable Player pSender, PaperNode node) {
        // Make sure that the item can actually be built
        ItemStack baseItem;
        try {
            baseItem = node.createItem(locale).handle();
        } catch (ItemCreationException e) {
            throw error("item_creation",
                    "message", Text.mergeMessages(e));
        }
        //noinspection ConstantConditions
        int amount = ctx.getOrDefault("amount", 1);
        List<Player> targets = targets(ctx, "targets", pSender);

        for (Player target : targets) {
            ItemStack item;
            // We shouldn't *need* this try/catch, but just in case...
            try {
                item = node.createItem(target.locale()).handle();
            } catch (ItemCreationException e) {
                throw error("item_creation",
                        "message", Text.mergeMessages(e));
            }
            Inventory inventory = target.getInventory();
            for (int i = 0; i < amount; i++)
                inventory.addItem(item);
        }

        send(sender, locale, "give",
                "amount", amount+"",
                "item", baseItem.displayName(),
                "target", targets.size() == 1
                        ? targets.get(0).displayName()
                        : targets.size()+"");
    }

    private void give(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, @Nullable Player pSender) {
        give(ctx, sender, locale, pSender, ctx.get("node"));
    }

    private void build(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, @Nullable Player pSender) {
        give(ctx, sender, locale, pSender, ctx.<PaperBlueprint>get("blueprint").create());
    }
}
