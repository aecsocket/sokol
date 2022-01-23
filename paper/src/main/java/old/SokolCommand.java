package old;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.flags.CommandFlag;
import cloud.commandframework.arguments.standard.EnumArgument;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.bukkit.parsers.PlayerArgument;
import cloud.commandframework.bukkit.parsers.selector.MultiplePlayerSelectorArgument;
import cloud.commandframework.context.CommandContext;

import com.github.aecsocket.sokol.core.Renderable;
import com.github.aecsocket.sokol.core.node.ItemCreationException;
import com.github.aecsocket.sokol.core.registry.Keyed;
import com.github.aecsocket.sokol.core.stat.Stat;
import com.github.aecsocket.sokol.core.stat.StatIntermediate;
import com.github.aecsocket.sokol.core.stat.StatMap;
import com.github.aecsocket.sokol.paper.impl.*;
import com.gitlab.aecsocket.minecommons.core.CollectionBuilder;
import com.gitlab.aecsocket.minecommons.core.Components;
import com.gitlab.aecsocket.minecommons.paper.plugin.BaseCommand;

import old.command.BlueprintArgument;
import old.command.ComponentArgument;
import old.command.NodeArgument;
import old.wrapper.user.PlayerUser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import old.impl.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.JoinConfiguration.*;

/* package */ final class SokolCommand extends BaseCommand<SokolPlugin> {
    private static final JoinConfiguration separator = separator(text(", ", NamedTextColor.GRAY));
    private static final List<String> registryTypes = List.of(
            PaperComponent.class.getSimpleName(),
            PaperBlueprint.class.getSimpleName()
    );

    SokolCommand(SokolPlugin plugin) throws Exception {
        super(plugin, "sokol",
                (mgr, root) -> mgr.commandBuilder(root, ArgumentDescription.of("Plugin main command.")));

        captions.registerMessageFactory(NodeArgument.ARGUMENT_PARSE_FAILURE_NODE_GENERIC, captionLocalizer);
        captions.registerMessageFactory(NodeArgument.ARGUMENT_PARSE_FAILURE_NODE_INVALID, captionLocalizer);
        captions.registerMessageFactory(ComponentArgument.ARGUMENT_PARSE_FAILURE_COMPONENT_GENERIC, captionLocalizer);
        captions.registerMessageFactory(ComponentArgument.ARGUMENT_PARSE_FAILURE_COMPONENT_INVALID, captionLocalizer);
        captions.registerMessageFactory(BlueprintArgument.ARGUMENT_PARSE_FAILURE_BLUEPRINT_GENERIC, captionLocalizer);
        captions.registerMessageFactory(BlueprintArgument.ARGUMENT_PARSE_FAILURE_BLUEPRINT_INVALID, captionLocalizer);

        manager.command(root
                .literal("list", ArgumentDescription.of("Lists all registered objects."))
                .flag(CommandFlag.newBuilder("type")
                        .withAliases("t")
                        .withArgument(StringArgument.<CommandSender>newBuilder("value")
                                .quoted()
                                .withSuggestionsProvider((ctx, inp) -> registryTypes))
                        .withDescription(ArgumentDescription.of("The type of object to get.")))
                .flag(CommandFlag.newBuilder("filter")
                        .withAliases("f")
                        .withArgument(StringArgument.newBuilder("value")
                                .quoted()
                                .withSuggestionsProvider((ctx, inp) -> CollectionBuilder.list(new ArrayList<>(plugin.components().keySet()))
                                        .add(plugin.blueprints().keySet())
                                        .get()))
                        .withDescription(ArgumentDescription.of("The filter for the name/ID.")))
                .permission("%s.command.list".formatted(rootName))
                .handler(c -> handle(c, this::list)));
        manager.command(root
                .literal("tree", ArgumentDescription.of("Shows a node tree of an equipped item."))
                .argument(EnumArgument.optional(EquipmentSlot.class, "slot"), ArgumentDescription.of("The slot to get the item in."))
                .argument(PlayerArgument.optional("target"), ArgumentDescription.of("The player to get the item from."))
                .flag(CommandFlag.newBuilder("contextless")
                        .withAliases("c")
                        .withDescription(ArgumentDescription.of("Removes the context of you being the user, when generating info.")))
                .permission("%s.command.tree".formatted(rootName))
                .handler(c -> handle(c, this::tree)));
        manager.command(root
                .literal("component", ArgumentDescription.of("Outputs detailed information on a registered component."))
                .argument(ComponentArgument.of(plugin, "component"), ArgumentDescription.of("The component to list information on."))
                .permission("%s.command.component".formatted(rootName))
                .handler(c -> handle(c, this::component)));
        manager.command(root
                .literal("blueprint", ArgumentDescription.of("Outputs detailed information on a registered blueprint."))
                .argument(BlueprintArgument.of(plugin, "blueprint"), ArgumentDescription.of("The blueprint to list information on."))
                .permission("%s.command.blueprint".formatted(rootName))
                .handler(c -> handle(c, this::blueprint)));
        manager.command(root
                .literal("give", ArgumentDescription.of("Gives a component/node tree to specified players."))
                .argument(MultiplePlayerSelectorArgument.of("targets"), ArgumentDescription.of("The players to give the item to."))
                .argument(NodeArgument.of(plugin, "node"), ArgumentDescription.of("The component/node tree to give."))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1).asOptional(), ArgumentDescription.of("The amount of the item to give."))
                .permission("%s.command.give".formatted(rootName))
                .handler(c -> handle(c, this::give)));
        manager.command(root
                .literal("build", ArgumentDescription.of("Gives a built blueprint to specified players."))
                .argument(MultiplePlayerSelectorArgument.of("targets"), ArgumentDescription.of("The players to give the item to."))
                .argument(BlueprintArgument.of(plugin, "blueprint"), ArgumentDescription.of("The blueprint to give."))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1).asOptional(), ArgumentDescription.of("The amount of the item to give."))
                .permission("%s.command.build".formatted(rootName))
                .handler(c -> handle(c, this::build)));
    }

    // Utils

    private <T extends Renderable> Component render(Locale locale, T obj, Function<T, String> getKey) {
        return obj.render(locale, lc)
                .hoverEvent(lc.safe(locale, PREFIX_COMMAND + ".hover.keyed",
                        "key", getKey.apply(obj),
                        "type", obj.getClass().getName()));
    }

    private Component renderKeyed(Locale locale, Keyed keyed) {
        return render(locale, keyed, Keyed::id);
    }

    private <T> void renderStats(Locale locale, List<Component> lines, String key, Stat.Node<T> node) {
        List<? extends Stat.Node<?>> chain = node.asList();
        lc.lines(locale, PREFIX_COMMAND + ".stats.entry",
                        "name", render(locale, node.stat(), e -> key),
                        "key", key,
                        "value", node.stat().renderValue(locale, lc, node.compute()),
                        "nodes", chain.size() + "",
                        "chain", join(separator, chain.stream().map(n -> n.value().render(locale, lc)).collect(Collectors.toList())))
                .ifPresent(lines::addAll);
    }

    private List<Component> renderStats(Locale locale, StatMap map) {
        List<Component> lines = new ArrayList<>();
        lc.lines(locale, PREFIX_COMMAND + ".stats.header",
                        "amount", map.size()+"")
                .ifPresent(lines::addAll);
        for (var entry : map.entrySet()) {
            renderStats(locale, lines, entry.getKey(), entry.getValue());
        }

        return lines;
    }

    private Component renderConstant(Locale locale, boolean value) {
        return lc.safe(locale, "constant." + value);
    }

    private Component renderKeyedInfo(Locale locale, Keyed keyed, String command) {
        return lc.lines(locale, PREFIX_COMMAND + ".hover.keyed_info",
                        "key", keyed.id(),
                        "type", keyed.getClass().getName(),
                        "command", command)
                .map(m -> join(separator(newline()), m))
                .orElse(empty());
    }

    // Commands

    private void list(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, @Nullable Player pSender) {
        String type = ctx.flags().get("type");
        if (type != null) type = type.toLowerCase(Locale.ROOT);
        String filter = ctx.flags().get("filter");
        if (filter != null) filter = filter.toLowerCase(Locale.ROOT);

        List<Keyed> registered = new ArrayList<>(plugin.components().values());
        registered.addAll(plugin.blueprints().values());
        int results = 0;
        for (var object : registered) {
            Component rendered = renderKeyed(locale, object);
            String id = object.id();
            if (type != null) {
                String typeName = object.getClass().getName().toLowerCase(Locale.ROOT);
                if (!typeName.contains(type))
                    continue;
            }
            if (filter != null) {
                String testId = id.toLowerCase(Locale.ROOT);
                String name = PlainTextComponentSerializer.plainText().serialize(rendered).toLowerCase(Locale.ROOT);
                if (!testId.contains(filter) && !name.contains(filter))
                    continue;
            }

            ++results;
            String command = "/" + rootName + " " + (
                    object instanceof PaperComponent ? "component"
                    : object instanceof PaperBlueprint ? "blueprint"
                    : "?") + " " + object.id();
            Component hover = renderKeyedInfo(locale, object, command);
            ClickEvent click = ClickEvent.runCommand(command);
            lc.lines(locale, PREFIX_COMMAND + ".list.entry",
                    "type", object.getClass().getSimpleName(),
                    "name", object.render(locale, lc),
                    "id", id)
                    .ifPresent(m -> m.forEach(c -> sender.sendMessage(c.hoverEvent(hover).clickEvent(click))));
        }
        send(sender, locale, "list.total",
                "results", ""+results);
    }

    private void tree0(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, @Nullable Player pSender, Component indent, int depth, PaperNode node) {
        for (var entry : node.nodes().entrySet()) {
            String key = entry.getKey();
            PaperNode child = entry.getValue();
            PaperComponent component = child.value();

            String command = "/" + rootName + " component " + component.id();
            Component hover = renderKeyedInfo(locale, component, command);
            ClickEvent click = ClickEvent.runCommand(command);
            lc.lines(locale, PREFIX_COMMAND + ".tree.node",
                            "indent", Components.repeat(indent, depth),
                            "slot", node.value().slot(key).orElseThrow(IllegalStateException::new).render(locale, lc),
                            "key", key,
                            "name", renderKeyed(locale, component),
                            "id", component.id())
                    .ifPresent(m -> m.forEach(c -> sender.sendMessage(c.hoverEvent(hover).clickEvent(click))));
            tree0(ctx, sender, locale, pSender, indent, depth + 1, child);
        }
    }

    private void tree(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, @Nullable Player pSender, PaperNode node, String type) {
        String command = "/" + rootName + " component " + node.value().id();
        Component hover = renderKeyedInfo(locale, node.value(), command);
        ClickEvent click = ClickEvent.runCommand(command);
        lc.lines(locale, PREFIX_COMMAND + ".tree." + type,
                        "type", node.value().getClass().getSimpleName(),
                        "name", renderKeyed(locale, node.value()),
                        "id", node.value().id())
                .ifPresent(m -> m.forEach(c -> sender.sendMessage(c.hoverEvent(hover).clickEvent(click))));
        tree0(ctx, sender, locale, pSender, lc.safe(locale, PREFIX_COMMAND + ".tree.indent"), 0, node);
    }

    private void tree(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, @Nullable Player pSender) {
        EquipmentSlot slot = ctx.getOrDefault("slot", EquipmentSlot.HAND);
        Player target = defaultedArg(ctx, "target", pSender, p -> p);

        //noinspection ConstantConditions
        ItemStack item = target.getInventory().getItem(slot);
        PaperNode node = plugin.persistence().safeLoad(item)
                .orElseThrow(() -> error("item_not_tree"));
        var treeData = pSender == null || ctx.flags().isPresent("contextless")
                ? node.build(locale)
                : node.build(PlayerUser.user(plugin, pSender));
        tree(ctx, sender, locale, pSender, node, "header");


        List<NodePath> incomplete = treeData.incomplete();
        if (incomplete.isEmpty())
            send(sender, locale, "tree.complete.complete");
        else {
            send(sender, locale, "tree.complete.incomplete",
                    "missing", ""+incomplete.size());
            for (var path : incomplete)
                send(sender, locale, "tree.complete.incomplete_entry",
                        "path", ""+path);
        }
        StatMap stats = treeData.stats();
        Component hover = join(separator(newline()), renderStats(locale, stats));
        lc.lines(locale, PREFIX_COMMAND + ".tree.stats",
                "amount", ""+stats.size())
                .ifPresent(m -> m.forEach(c -> sender.sendMessage(c.hoverEvent(hover))));
    }

    private void component(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, @Nullable Player pSender) {
        PaperComponent component = ctx.get("component");

        send(sender, locale, "tree.header",
                "type", component.getClass().getSimpleName(),
                "name", renderKeyed(locale, component),
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
                    "name", render(locale, slot, e -> key),
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
                    "name", renderKeyed(locale, feature),
                    "id", id,
                    "description", feature.renderDescription(locale, lc))
                    .ifPresent(m -> m.forEach(c -> sender.sendMessage(c.hoverEvent(hover))));
        }

        List<StatIntermediate.MapData> stats = component.stats().join();
        send(sender, locale, "component.stats",
                "amount", stats.size()+"");
        for (var data : stats) {
            StatMap map = data.stats();
            List<Component> lines = renderStats(locale, data.stats());
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
                "name", renderKeyed(locale, blueprint),
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
            if (pSender == null)
                baseItem = node.createItem(locale).handle();
            else
                baseItem = node.createItem(PlayerUser.user(plugin, pSender)).handle();
        } catch (ItemCreationException e) {
            throw error("item_creation", e);
        }
        //noinspection ConstantConditions
        int amount = ctx.getOrDefault("amount", 1);
        List<Player> targets = targets(ctx, "targets", pSender);

        for (Player target : targets) {
            ItemStack item;
            // We shouldn't *need* this try/catch, but just in case...
            try {
                item = node.createItem(PlayerUser.user(plugin, target)).handle();
            } catch (ItemCreationException e) {
                throw error("item_creation", e);
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
