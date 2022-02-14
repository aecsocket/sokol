package com.github.aecsocket.sokol.paper;

import cloud.commandframework.ArgumentDescription;

import cloud.commandframework.arguments.standard.EnumArgument;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.bukkit.parsers.PlayerArgument;
import cloud.commandframework.bukkit.parsers.selector.MultiplePlayerSelectorArgument;
import cloud.commandframework.context.CommandContext;
import com.github.aecsocket.minecommons.core.Colls;
import com.github.aecsocket.minecommons.core.Components;
import com.github.aecsocket.minecommons.core.i18n.I18N;
import com.github.aecsocket.minecommons.core.i18n.Renderable;
import com.github.aecsocket.minecommons.core.node.NodePath;
import com.github.aecsocket.minecommons.paper.plugin.BaseCommand;
import com.github.aecsocket.sokol.core.Tree;
import com.github.aecsocket.sokol.core.context.Context;
import com.github.aecsocket.sokol.core.registry.Keyed;
import com.github.aecsocket.sokol.core.stat.StatIntermediate;
import com.github.aecsocket.sokol.core.stat.StatMap;
import com.github.aecsocket.sokol.core.world.ItemCreationException;
import com.github.aecsocket.sokol.paper.command.BlueprintArgument;
import com.github.aecsocket.sokol.paper.command.BlueprintNodeArgument;
import com.github.aecsocket.sokol.paper.command.ComponentArgument;
import com.github.aecsocket.sokol.paper.context.PaperContext;
import com.github.aecsocket.sokol.paper.world.PaperItemUser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Locale;

import static net.kyori.adventure.text.Component.*;

/* package */ final class SokolCommand extends BaseCommand<SokolPlugin> {
    private static final JoinConfiguration NEWLINE = JoinConfiguration.separator(newline());
    private static final List<String> REGISTRY_TYPES = List.of(
        PaperComponent.class.getSimpleName(),
        PaperBlueprint.class.getSimpleName()
    );

    public static final String
        KEYED_HOVER = "keyed.hover",
        FEATURE_DESCRIPTION = "feature_description",
        ERROR_ITEM_CREATION = "error.item_creation",
        ERROR_ITEM_NOT_TREE = "error.item_not_tree",
        COMMAND_LIST_ENTRY = "command.list.entry",
        COMMAND_LIST_TOTAL = "command.list.total",
        COMMAND_INFO_HEADER = "command.info.header",
        COMMAND_INFO_DESCRIPTION = "command.info.description",
        COMMAND_COMPONENT_TAGS = "command.component.tags",
        COMMAND_COMPONENT_SLOTS = "command.component.slots",
        COMMAND_COMPONENT_SLOT = "command.component.slot",
        COMMAND_COMPONENT_FEATURES = "command.component.features",
        COMMAND_COMPONENT_FEATURE = "command.component.feature",
        COMMAND_COMPONENT_STATS = "command.component.stats",
        COMMAND_COMPONENT_STAT = "command.component.stat",
        COMMAND_TREE_ROOT = "command.tree.root",
        COMMAND_TREE_INDENT = "command.tree.indent",
        COMMAND_TREE_EMPTY = "command.tree.empty",
        COMMAND_TREE_CHILD = "command.tree.child",
        COMMAND_TREE_CHILD_HOVER = "command.tree.child_hover",
        COMMAND_TREE_STATS = "command.tree.stats",
        COMMAND_TREE_INCOMPLETE_HEADER = "command.tree.incomplete.header",
        COMMAND_TREE_INCOMPLETE_ENTRY = "command.tree.incomplete.entry",
        COMMAND_GIVE = "command.give";

    public SokolCommand(SokolPlugin plugin) throws Exception {
        super(plugin, "sokol",
                (mgr, root) -> mgr.commandBuilder(root, ArgumentDescription.of("Plugin main command.")));

        registerCaption(BlueprintNodeArgument.ARGUMENT_PARSE_FAILURE_BLUEPRINT_NODE_REGISTRY);
        registerCaption(BlueprintNodeArgument.ARGUMENT_PARSE_FAILURE_BLUEPRINT_NODE_GENERIC);
        registerCaption(ComponentArgument.ARGUMENT_PARSE_FAILURE_COMPONENT_REGISTRY);
        registerCaption(BlueprintArgument.ARGUMENT_PARSE_FAILURE_BLUEPRINT_REGISTRY);

        manager.command(root
            .literal("list", ArgumentDescription.of("List all registered objects."))
            .flag(manager.flagBuilder("type").withAliases("t")
                .withArgument(StringArgument.newBuilder("value")
                    .quoted()
                    .withSuggestionsProvider((ctx, inp) -> REGISTRY_TYPES))
                .withDescription(ArgumentDescription.of("The type of object to get.")))
            .flag(manager.flagBuilder("filter").withAliases("f")
                .withArgument(StringArgument.newBuilder("value")
                    .quoted()
                    .withSuggestionsProvider((ctx, inp) -> Colls.joinList(plugin.components().keySet(), plugin.blueprints().keySet())))
                .withDescription(ArgumentDescription.of("The filter for the name or ID.")))
            .permission(permission("list"))
            .handler(c -> handle(c, this::list)));
        manager.command(root
            .literal("component", ArgumentDescription.of("Shows information on a component."))
            .argument(ComponentArgument.of(plugin, "object"), ArgumentDescription.of("The component to list information on."))
            .permission(permission("component"))
            .handler(c -> handle(c, this::component)));
        manager.command(root
            .literal("blueprint", ArgumentDescription.of("Shows information on a blueprint."))
            .argument(BlueprintArgument.of(plugin, "object"), ArgumentDescription.of("The blueprint to list information on."))
            .permission(permission("blueprint"))
            .handler(c -> handle(c, this::blueprint)));
        manager.command(root
            .literal("tree", ArgumentDescription.of("Shows information on the currently held node tree."))
            .argument(EnumArgument.optional(EquipmentSlot.class, "slot"), ArgumentDescription.of("The slot to get the item from."))
            .argument(PlayerArgument.optional("target"), ArgumentDescription.of("The player to get the item from."))
            .permission(permission("tree"))
            .handler(c -> handle(c, this::tree)));
        manager.command(root
            .literal("give", ArgumentDescription.of("Gives a blueprint tree to players."))
            .argument(MultiplePlayerSelectorArgument.of("targets"), ArgumentDescription.of("The players to give the item to."))
            .argument(BlueprintNodeArgument.of(plugin, "node"), ArgumentDescription.of("The blueprint tree to give."))
            .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1).asOptional(),
                ArgumentDescription.of("The amount of the item to give."))
            .permission(permission("give"))
            .handler(c -> handle(c, this::give)));
        manager.command(root
            .literal("build", ArgumentDescription.of("Builds a registered blueprint and gives it to players."))
            .argument(MultiplePlayerSelectorArgument.of("targets"), ArgumentDescription.of("The players to give the item to."))
            .argument(BlueprintArgument.of(plugin, "blueprint"), ArgumentDescription.of("The blueprint to build."))
            .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1).asOptional(),
                ArgumentDescription.of("The amount of the item to give."))
            .permission(permission("build"))
            .handler(c -> handle(c, this::build)));
    }

    private <T extends Renderable & Keyed> Component renderKeyedHover(Locale locale, T obj, String command) {
        return join(NEWLINE, i18n.lines(locale, KEYED_HOVER,
            c -> c.of("id", () -> text(obj.id())),
            c -> c.of("type", () -> text(obj.getClass().getName())),
            c -> c.of("command", () -> text(command))));
    }

    private String command(PaperComponent object) {
        return "/%s component %s".formatted(rootName, object.id());
    }

    private String command(PaperBlueprint object) {
        return "/%s blueprint %s".formatted(rootName, object.id());
    }

    private @Nullable String command(Keyed object) {
        return object instanceof PaperComponent cast
            ? command(cast)
            : object instanceof PaperBlueprint cast
            ? command(cast)
            : null;
    }

    private void list(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, @Nullable Player pSender) {
        String type = ctx.flags().get("type");
        if (type != null) type = type.toLowerCase(Locale.ROOT);
        String filter = ctx.flags().get("filter");
        if (filter != null) filter = filter.toLowerCase(Locale.ROOT);

        List<Keyed> registered = Colls.joinList(plugin.components().values(), plugin.blueprints().values());
        int results = 0;
        for (var object : registered) {
            if (type != null) {
                String typeName = object.getClass().getName().toLowerCase(Locale.ROOT);
                if (!typeName.contains(type))
                    continue;
            }
            Component rendered = object.render(i18n, locale);
            String id = object.id();
            if (filter != null) {
                if (
                    !id.toLowerCase(Locale.ROOT).contains(filter)
                    && !PlainTextComponentSerializer.plainText().serialize(rendered).toLowerCase(Locale.ROOT).contains(filter)
                )
                    continue;
            }

            ++results;
            String command = command(object);
            Component hover = command == null ? null : renderKeyedHover(locale, object, command);
            ClickEvent click = command == null ? null : ClickEvent.runCommand(command);
            plugin.send(sender, i18n.modLines(locale, COMMAND_LIST_ENTRY,
                line -> line.hoverEvent(hover).clickEvent(click),
                c -> c.of("type", () -> text(object.getClass().getSimpleName())),
                c -> c.of("name", () -> c.rd(object)),
                c -> c.of("id", () -> text(id))));
        }

        int fResults = results;
        plugin.send(sender, i18n.lines(locale, COMMAND_LIST_TOTAL,
            c -> c.of("amount", () -> text(fResults))));
    }

    private void component(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, @Nullable Player pSender) {
        PaperComponent object = ctx.get("object");

        info(ctx, sender, locale, pSender, object);

        plugin.send(sender, i18n.lines(locale, COMMAND_COMPONENT_TAGS,
            c -> c.of("tags", () -> text(String.join(", ", object.tags())))));

        plugin.send(sender, i18n.lines(locale, COMMAND_COMPONENT_SLOTS,
            c -> c.of("amount", () -> text(object.slots().size()))));
        for (var entry : object.slots().entrySet()) {
            String id = entry.getKey();
            PaperNodeSlot slot = entry.getValue();

            Component hover = slot.rule().render(i18n, locale);
            plugin.send(sender, i18n.modLines(locale, COMMAND_COMPONENT_SLOT,
                line -> line.hoverEvent(hover),
                c -> c.of("name", () -> c.rd(slot)),
                c -> c.of("id", () -> text(id)),
                c -> c.of("tags", () -> text(String.join(", ", slot.tags()))),
                c -> c.of("offset", () -> text(""+slot.offset()))));
        }

        plugin.send(sender, i18n.lines(locale, COMMAND_COMPONENT_FEATURES,
            c -> c.of("amount", () -> text(object.features().size()))));
        for (var entry : object.features().entrySet()) {
            String id = entry.getKey();
            PaperFeatureProfile<?, ?> profile = entry.getValue();

            Component hover = profile.type().renderDescription(i18n, locale)
                .map(lines -> Component.join(NEWLINE, lines.stream()
                    .map(line -> i18n.line(locale, FEATURE_DESCRIPTION,
                        c -> c.of("line", () -> line)))
                    .toList()))
                .orElse(null);
            plugin.send(sender, i18n.modLines(locale, COMMAND_COMPONENT_FEATURE,
                line -> line.hoverEvent(hover),
                c -> c.of("name", () -> c.rd(profile.type())),
                c -> c.of("id", () -> text(id))));
        }

        List<StatIntermediate.MapData> allData = object.stats().join();
        plugin.send(sender, i18n.lines(locale, COMMAND_COMPONENT_STATS,
            c -> c.of("amount", () -> text(allData.size()))));
        for (var data : allData) {
            StatMap stats = data.entries();

            Component hover = join(NEWLINE, stats.render(i18n, locale));
            plugin.send(sender, i18n.modLines(locale, COMMAND_COMPONENT_STAT,
                line -> line.hoverEvent(hover),
                c -> c.of("amount", () -> text(stats.size())),
                c -> c.of("priority", () -> c.rd(data.priority())),
                c -> c.of("rule", () -> c.rd(data.rule()))));
        }
    }

    private void blueprint(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, @Nullable Player pSender) {
        PaperBlueprint object = ctx.get("object");

        info(ctx, sender, locale, pSender, object);

        tree(ctx, sender, locale, pSender, object.create(), 1);
    }

    private void info(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, @Nullable Player pSender, Keyed object) {
        plugin.send(sender, i18n.lines(locale, COMMAND_INFO_HEADER,
            c -> c.of("type", () -> text(object.getClass().getSimpleName())),
            c -> c.of("name", () -> c.rd(object)),
            c -> c.of("id", () -> text(object.id()))));

        object.renderDescription(i18n, locale).ifPresent(desc -> {
            for (var line : desc) {
                plugin.send(sender, i18n.lines(locale, COMMAND_INFO_DESCRIPTION,
                    c -> c.of("line", () -> line)));
            }
        });
    }

    private void tree(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, @Nullable Player pSender) {
        EquipmentSlot slot = ctx.getOrDefault("slot", EquipmentSlot.HAND);
        Player target = defaultedArg(ctx, "target", pSender, p -> p);

        @SuppressWarnings("ConstantConditions")
        ItemStack item = target.getInventory().getItem(slot);
        PaperBlueprintNode node = plugin.persistence().load(item)
            .orElseThrow(() -> error(ERROR_ITEM_NOT_TREE));

        tree(ctx, sender, locale, pSender, node, 0);
    }

    private void subTree(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, @Nullable Player pSender, Component indent, int depth, PaperBlueprintNode node) {
        for (var entry : node.value().slots().entrySet()) {
            String key = entry.getKey();
            PaperNodeSlot slot = entry.getValue();
            node.get(slot.key()).ifPresentOrElse(child -> {
                String command = command(child.value());
                I18N.TemplateFactory[] templates = new I18N.TemplateFactory[] {
                    c -> c.of("indent", () -> Components.repeat(indent, depth)),
                    c -> c.of("slot", () -> c.rd(slot)),
                    c -> c.of("key", () -> text(key)),
                    c -> c.of("name", () -> c.rd(child.value())),
                    c -> c.of("id", () -> text(child.value().id()))
                };
                Component hover = i18n.line(locale, COMMAND_TREE_CHILD_HOVER, templates)
                    .append(renderKeyedHover(locale, child.value(), command));
                ClickEvent click = ClickEvent.runCommand(command);
                plugin.send(sender, i18n.modLines(locale, COMMAND_TREE_CHILD,
                    line -> line.hoverEvent(hover).clickEvent(click),
                    templates));
                subTree(ctx, sender, locale, pSender, indent, depth + 1, child);
            }, () -> plugin.send(sender, i18n.lines(locale, COMMAND_TREE_EMPTY,
                c -> c.of("indent", () -> Components.repeat(indent, depth)),
                c -> c.of("slot", () -> c.rd(slot)),
                c -> c.of("key", () -> text(key)))));
        }
    }

    private void tree(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, @Nullable Player pSender, PaperBlueprintNode node, int depth) {
        String command = command(node.value());
        Component indent = i18n.line(locale, COMMAND_TREE_INDENT);
        Component rootHover = renderKeyedHover(locale, node.value(), command);
        ClickEvent rootClick = ClickEvent.runCommand(command);
        plugin.send(sender, i18n.modLines(locale, COMMAND_TREE_ROOT,
            line -> line.hoverEvent(rootHover).clickEvent(rootClick),
            c -> c.of("indent", () -> Components.repeat(indent, depth)),
            c -> c.of("name", () -> c.rd(node.value())),
            c -> c.of("id", () -> text(node.value().id()))));
        subTree(ctx, sender, locale, pSender, indent, depth, node);

        Tree<PaperTreeNode> tree = node.asTreeNode(Context.context(locale)).tree();
        StatMap stats = tree.stats();
        List<NodePath> incomplete = tree.incomplete();

        Component statsHover = Component.join(NEWLINE, stats.render(i18n, locale));
        plugin.send(sender, i18n.modLines(locale, COMMAND_TREE_STATS,
            line -> line.hoverEvent(statsHover),
            c -> c.of("amount", () -> text(stats.size()))));
        plugin.send(sender, i18n.lines(locale, COMMAND_TREE_INCOMPLETE_HEADER,
            c -> c.of("amount", () -> text(incomplete.size()))));
        for (var path : incomplete) {
            plugin.send(sender, i18n.lines(locale, COMMAND_TREE_INCOMPLETE_ENTRY,
                c -> c.of("path", () -> text(String.join("/", path)))));
        }
    }

    private void give(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, @Nullable Player pSender, PaperBlueprintNode blueprint) {
        ItemStack baseItem;
        try {
            baseItem = blueprint.asTreeNode(
                pSender == null
                    ? Context.context(locale)
                    : PaperContext.context(PaperItemUser.user(plugin, pSender))
            ).asItem().handle();
        } catch (ItemCreationException e) {
            throw error(ERROR_ITEM_CREATION, e);
        }

        @SuppressWarnings("ConstantConditions")
        int amount = ctx.getOrDefault("amount", 1);
        List<Player> targets = targets(ctx, "targets", pSender);

        for (var target : targets) {
            ItemStack item = blueprint.asTreeNode(PaperContext.context(PaperItemUser.user(plugin, target)))
                .asItem().handle();
            Inventory inventory = target.getInventory();
            for (int i = 0; i < amount; i++) {
                inventory.addItem(item);
            }
        }

        plugin.send(sender, i18n.lines(locale, COMMAND_GIVE,
            c -> c.of("amount", () -> text(amount)),
            c -> c.of("item", baseItem::displayName),
            c -> c.of("targets", () -> targets.size() == 1
                ? targets.get(0).displayName()
                : text(targets.size()))));
    }

    private void give(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, @Nullable Player pSender) {
        give(ctx, sender, locale, pSender, ctx.get("node"));
    }

    private void build(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, @Nullable Player pSender) {
        give(ctx, sender, locale, pSender, ctx.<PaperBlueprint>get("blueprint").create());
    }
}
