package com.github.aecsocket.sokol.paper;

import cloud.commandframework.ArgumentDescription;

import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.bukkit.parsers.selector.MultiplePlayerSelectorArgument;
import cloud.commandframework.context.CommandContext;
import com.github.aecsocket.minecommons.paper.plugin.BaseCommand;
import com.github.aecsocket.sokol.core.context.Context;
import com.github.aecsocket.sokol.core.world.ItemCreationException;
import com.github.aecsocket.sokol.paper.command.BlueprintNodeArgument;
import com.github.aecsocket.sokol.paper.context.PaperContext;
import com.github.aecsocket.sokol.paper.world.PaperItemUser;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Locale;

/* package */ final class SokolCommand extends BaseCommand<SokolPlugin> {
    public static final String
        ERROR_ITEM_CREATION = "error.item_creation",
        COMMAND_GIVE = "command.give";

    public SokolCommand(SokolPlugin plugin) throws Exception {
        super(plugin, "sokol",
                (mgr, root) -> mgr.commandBuilder(root, ArgumentDescription.of("Plugin main command.")));

        registerCaption(BlueprintNodeArgument.ARGUMENT_PARSE_FAILURE_BLUEPRINT_NODE_REGISTRY);
        registerCaption(BlueprintNodeArgument.ARGUMENT_PARSE_FAILURE_BLUEPRINT_NODE_GENERIC);
        registerCaption(BlueprintNodeArgument.ARGUMENT_PARSE_FAILURE_BLUEPRINT_NODE_INVALID);

        manager.command(root
            .literal("give", ArgumentDescription.of("Gives a blueprint tree to players."))
            .argument(MultiplePlayerSelectorArgument.of("targets"), ArgumentDescription.of("The players to give the item to."))
            .argument(BlueprintNodeArgument.of(plugin, "node"), ArgumentDescription.of("The blueprint tree to give."))
            .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1).asOptional(),
                ArgumentDescription.of("The amount of the item to give."))
            .permission("%s.command.give".formatted(rootName))
            .handler(c -> handle(c, this::give)));
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

        send(sender, locale, COMMAND_GIVE,
            c -> c.of("amount", ""+amount),
            c -> c.of("item", baseItem.displayName()),
            c -> c.of("targets", targets.size() == 1
                ? targets.get(0).displayName()
                : ""+targets.size()));
    }
}
