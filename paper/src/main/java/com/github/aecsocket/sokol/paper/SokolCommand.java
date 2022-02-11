package com.github.aecsocket.sokol.paper;

import cloud.commandframework.ArgumentDescription;

import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.bukkit.parsers.selector.MultiplePlayerSelectorArgument;
import cloud.commandframework.context.CommandContext;
import com.github.aecsocket.minecommons.core.Colls;
import com.github.aecsocket.minecommons.core.i18n.Renderable;
import com.github.aecsocket.minecommons.paper.plugin.BaseCommand;
import com.github.aecsocket.sokol.core.context.Context;
import com.github.aecsocket.sokol.core.registry.Keyed;
import com.github.aecsocket.sokol.core.world.ItemCreationException;
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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Locale;

import static net.kyori.adventure.text.Component.*;

/* package */ final class SokolCommand extends BaseCommand<SokolPlugin> {
    private static final JoinConfiguration NEWLINE = JoinConfiguration.separator(newline());
    private static final List<String> REGISTRY_TYPES = List.of(
        PaperComponent.class.getSimpleName()
        // todo bp
    );

    public static final String
        KEYED_HOVER = "keyed.hover",
        ERROR_ITEM_CREATION = "error.item_creation",
        COMMAND_GIVE = "command.give",
        COMMAND_LIST_ENTRY = "command.list.entry",
        COMMAND_LIST_TOTAL = "command.list.total";

    public SokolCommand(SokolPlugin plugin) throws Exception {
        super(plugin, "sokol",
                (mgr, root) -> mgr.commandBuilder(root, ArgumentDescription.of("Plugin main command.")));

        registerCaption(BlueprintNodeArgument.ARGUMENT_PARSE_FAILURE_BLUEPRINT_NODE_REGISTRY);
        registerCaption(BlueprintNodeArgument.ARGUMENT_PARSE_FAILURE_BLUEPRINT_NODE_GENERIC);
        registerCaption(ComponentArgument.ARGUMENT_PARSE_FAILURE_COMPONENT_REGISTRY);

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
                    .withSuggestionsProvider((ctx, inp) -> Colls.joinList(plugin.components().keySet()))) // TODO add bps
                .withDescription(ArgumentDescription.of("The filter for the name or ID.")))
            .permission(permission("list"))
            .handler(c -> handle(c, this::list)));
        manager.command(root
            .literal("component", ArgumentDescription.of("Shows information on a component."))
            .argument(ComponentArgument.of(plugin, "component"), ArgumentDescription.of("The component to list information on."))
            .permission(permission("component"))
            .handler(c -> handle(c, this::component)));
        manager.command(root
            .literal("give", ArgumentDescription.of("Gives a blueprint tree to players."))
            .argument(MultiplePlayerSelectorArgument.of("targets"), ArgumentDescription.of("The players to give the item to."))
            .argument(BlueprintNodeArgument.of(plugin, "node"), ArgumentDescription.of("The blueprint tree to give."))
            .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1).asOptional(),
                ArgumentDescription.of("The amount of the item to give."))
            .permission(permission("give"))
            .handler(c -> handle(c, this::give)));
    }

    private <T extends Renderable & Keyed> Component renderKeyedHover(Locale locale, T obj, String command) {
        return Component.join(NEWLINE, i18n.lines(locale, KEYED_HOVER,
            c -> c.of("id", obj.id()),
            c -> c.of("type", obj.getClass().getName()),
            c -> c.of("command", command)));
    }

    private void list(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, @Nullable Player pSender) {
        String type = ctx.flags().get("type");
        if (type != null) type = type.toLowerCase(Locale.ROOT);
        String filter = ctx.flags().get("filter");
        if (filter != null) filter = filter.toLowerCase(Locale.ROOT);

        List<Keyed> registered = Colls.joinList(plugin.components().values()); // TODO add bps
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
            String command = "/%s %s %s".formatted(
                rootName,
                object instanceof PaperComponent ? "component" : "?", // todo add bps
                object.id());
            Component hover = renderKeyedHover(locale, object, command);
            ClickEvent click = ClickEvent.runCommand(command);
            plugin.send(sender, i18n.modLines(locale, COMMAND_LIST_ENTRY,
                line -> line.hoverEvent(hover).clickEvent(click),
                c -> c.of("type", object.getClass().getSimpleName()),
                c -> c.of("name", object.render(i18n, locale)),
                c -> c.of("id", id)));
        }

        int fResults = results;
        plugin.send(sender, i18n.lines(locale, COMMAND_LIST_TOTAL,
            c -> c.of("results", ""+fResults)));
    }

    private void component(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, @Nullable Player pSender) {
        // todo impl
    }

    private void give(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, @Nullable Player pSender) {
        PaperBlueprintNode blueprint = ctx.get("node");

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
            c -> c.of("amount", ""+amount),
            c -> c.of("item", baseItem.displayName()),
            c -> c.of("targets", targets.size() == 1
                ? targets.get(0).displayName()
                : Component.text(""+targets.size()))));
    }
}
