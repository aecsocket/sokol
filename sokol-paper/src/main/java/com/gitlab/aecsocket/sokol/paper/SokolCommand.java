package com.gitlab.aecsocket.sokol.paper;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.flags.CommandFlag;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.bukkit.arguments.selector.MultiplePlayerSelector;
import cloud.commandframework.bukkit.parsers.selector.MultiplePlayerSelectorArgument;
import cloud.commandframework.captions.SimpleCaptionRegistry;
import cloud.commandframework.context.CommandContext;
import com.gitlab.aecsocket.minecommons.paper.plugin.BaseCommand;
import com.gitlab.aecsocket.sokol.core.system.ItemSystem;
import com.gitlab.aecsocket.sokol.paper.command.ComponentArgument;
import com.gitlab.aecsocket.sokol.paper.command.TreeArgument;
import com.gitlab.aecsocket.sokol.paper.system.PaperItemSystem;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;
import java.util.function.Supplier;

/* package */ class SokolCommand extends BaseCommand<SokolPlugin> {
    public SokolCommand(SokolPlugin plugin) throws Exception {
        super(plugin, "sokol",
                (mgr, root) -> mgr.commandBuilder(root, ArgumentDescription.of("Plugin main command.")));

        SimpleCaptionRegistry<CommandSender> captions = (SimpleCaptionRegistry<CommandSender>) manager.getCaptionRegistry();
        captions.registerMessageFactory(ComponentArgument.ARGUMENT_PARSE_FAILURE_COMPONENT, (c, s) -> "No component with ID [{input}]");
        captions.registerMessageFactory(ComponentArgument.ARGUMENT_PARSE_FAILURE_COMPONENT_INVALID, (c, s) -> "Component [{input}] does not meet requirements of command");

        captions.registerMessageFactory(TreeArgument.ARGUMENT_PARSE_FAILURE_TREE, (c, s) -> "Could not create tree: {exception}");
        captions.registerMessageFactory(TreeArgument.ARGUMENT_PARSE_FAILURE_TREE_INVALID, (c, s) -> "Tree of component [{id}] does not meet requirements of command");

        manager.command(root
                .literal("give", ArgumentDescription.of("Gives an item-applicable component to players."))
                .argument(MultiplePlayerSelectorArgument.of("targets"), ArgumentDescription.of("The players to give the component to."))
                .argument(ComponentArgument.<CommandSender>newBuilder(plugin, "component")
                        .test(c -> c.baseSystems().containsKey(ItemSystem.ID))
                        .asOptional(), ArgumentDescription.of("The component to give, or the currently held component if not specified."))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1).asOptional(), ArgumentDescription.of("The amount of the component to give."))
                .handler(c -> handle(c, this::give)));

        manager.command(root
                .literal("create", ArgumentDescription.of("Creates a tree of, and gives, an item-applicable component to players, using HOCON syntax."))
                .argument(MultiplePlayerSelectorArgument.of("targets"), ArgumentDescription.of("The players to give the component to."))
                .argument(TreeArgument.<CommandSender>newBuilder(plugin, "tree")
                        .test(c -> c.value().baseSystems().containsKey(ItemSystem.ID))
                        .asOptional(), ArgumentDescription.of("The component tree to give, or the currently held component tree if not specified."))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1).asOptional(), ArgumentDescription.of("The amount of the component to give."))
                .handler(c -> handle(c, this::create)));

        manager.command(root
                .literal("gui", ArgumentDescription.of("Opens the slot view GUI for a component."))
                .argument(MultiplePlayerSelectorArgument.optional("targets"), ArgumentDescription.of("The players to open the GUI for."))
                .argument(TreeArgument.<CommandSender>newBuilder(plugin, "tree")
                        .test(c -> c.value().baseSystems().containsKey(ItemSystem.ID))
                        .asOptional(), ArgumentDescription.of("The component tree to open, or the currently held component if not specified."))
                .flag(CommandFlag.newBuilder("modification")
                        .withAliases("m").withDescription(ArgumentDescription.of("If the component should be able to be modified by modifying slots.")))
                .flag(CommandFlag.newBuilder("limited")
                        .withAliases("l").withDescription(ArgumentDescription.of("If only field-modifiable slots can be modified.")))
                .handler(c -> handle(c, this::gui)));
    }
    // Utils

    private interface CommandHandler {
        void handle(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, Player pSender);
    }

    private static class CommandException extends RuntimeException {
        private final String key;
        private final Object[] args;

        public CommandException(String key, Object[] args) {
            this.key = key;
            this.args = args;
        }

        public String key() { return key; }
        public Object[] args() { return args; }
    }

    private static CommandException error(String key, Object... args) {
        return new CommandException("chat.error." + key, args);
    }

    private void handle(CommandContext<CommandSender> ctx, CommandHandler handler) {
        CommandSender sender = ctx.getSender();
        Locale locale = locale(sender);
        try {
            handler.handle(ctx, sender, locale, sender instanceof Player player ? player : null);
        } catch (CommandException e) {
            sender.sendMessage(localize(locale, e.key, e.args));
        }
    }

    private <T> T defaultedArg(CommandContext<CommandSender> ctx, String key, Player pSender, Supplier<T> ifPlayer) {
        return ctx.<T>getOptional(key).orElseGet(() -> {
            T result = pSender == null ? null : ifPlayer.get();
            if (result == null)
                throw error("no_arg", "arg", key);
            return result;
        });
    }

    private List<Player> targets(CommandContext<CommandSender> ctx, String key, Player pSender) {
        List<Player> targets = this.defaultedArg(ctx, key, pSender, () -> new MultiplePlayerSelector("", Collections.singletonList(pSender))).getPlayers();
        if (targets.size() == 0)
            throw error("no_targets");
        return targets;
    }

    // Command-specific Utils

    private void give(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, Player pSender, PaperTreeNode tree) {
        ItemStack item = tree
                .<PaperItemSystem.Instance>systemOf(ItemSystem.ID)
                .create(locale).handle();
        int amount = ctx.getOrDefault("amount", 1);
        List<Player> targets = targets(ctx, "targets", pSender);

        for (Player player : targets) {
            PlayerInventory inventory = player.getInventory();
            for (int i = 0; i < amount; i++) {
                inventory.addItem(item);
            }
        }
        sender.sendMessage(localize(locale, "chat.give",
                "amount", Integer.toString(amount),
                "component", tree.value().name(locale),
                "target", targets.size() == 1 ? targets.get(0).displayName() : Integer.toString(targets.size())));
    }

    // Commands

    private void give(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, Player pSender) {
        give(ctx, sender, locale, pSender, defaultedArg(ctx, "component", pSender,
                () -> plugin.persistenceManager().load(pSender.getInventory().getItemInMainHand()).value())
                .asTree());
    }

    private void create(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, Player pSender) {
        give(ctx, sender, locale, pSender, defaultedArg(ctx, "tree", pSender,
                () -> plugin.persistenceManager().load(pSender.getInventory().getItemInMainHand())));
    }

    private void gui(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, Player pSender) {
        List<Player> targets = targets(ctx, "targets", pSender);
        PaperTreeNode tree = defaultedArg(ctx, "tree", pSender,
                () -> plugin.persistenceManager().load(pSender.getInventory().getItemInMainHand()));

        for (Player target : targets) {
            plugin.createSlotViewGui(tree, target.locale(), pane -> pane
                    .modification(ctx.flags().isPresent("modification"))
                    .limited(ctx.flags().isPresent("limited")))
                    .show(target);
        }
    }
}
