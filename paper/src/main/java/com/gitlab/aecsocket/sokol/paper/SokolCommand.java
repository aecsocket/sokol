package com.gitlab.aecsocket.sokol.paper;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.flags.CommandFlag;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.bukkit.parsers.selector.MultiplePlayerSelectorArgument;
import cloud.commandframework.captions.SimpleCaptionRegistry;
import cloud.commandframework.context.CommandContext;
import com.gitlab.aecsocket.minecommons.paper.plugin.BaseCommand;
import com.gitlab.aecsocket.sokol.core.system.ItemSystem;
import com.gitlab.aecsocket.sokol.core.tree.AbstractTreeNode;
import com.gitlab.aecsocket.sokol.paper.command.BlueprintArgument;
import com.gitlab.aecsocket.sokol.paper.command.ComponentArgument;
import com.gitlab.aecsocket.sokol.paper.command.TreeArgument;
import com.gitlab.aecsocket.sokol.paper.slotview.SlotViewPane;
import com.gitlab.aecsocket.sokol.paper.system.impl.PaperItemSystem;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;

/* package */ class SokolCommand extends BaseCommand<SokolPlugin> {
    public SokolCommand(SokolPlugin plugin) throws Exception {
        super(plugin, "sokol",
                (mgr, root) -> mgr.commandBuilder(root, ArgumentDescription.of("Plugin main command.")));

        SimpleCaptionRegistry<CommandSender> captions = (SimpleCaptionRegistry<CommandSender>) manager.getCaptionRegistry();
        captions.registerMessageFactory(ComponentArgument.ARGUMENT_PARSE_FAILURE_COMPONENT, (c, s) -> "No component with ID [{input}]");
        captions.registerMessageFactory(ComponentArgument.ARGUMENT_PARSE_FAILURE_COMPONENT_INVALID, (c, s) -> "Component [{input}] does not meet requirements of command");

        captions.registerMessageFactory(TreeArgument.ARGUMENT_PARSE_FAILURE_TREE, (c, s) -> "Could not create tree: {exception}");
        captions.registerMessageFactory(TreeArgument.ARGUMENT_PARSE_FAILURE_TREE_INVALID, (c, s) -> "Tree of component [{id}] does not meet requirements of command");

        captions.registerMessageFactory(BlueprintArgument.ARGUMENT_PARSE_FAILURE_BLUEPRINT, (c, s) -> "No blueprint with ID [{input}]");
        captions.registerMessageFactory(BlueprintArgument.ARGUMENT_PARSE_FAILURE_BLUEPRINT_INVALID, (c, s) -> "Blueprint [{input}] does not meet requirements of command");

        manager.command(root
                .literal("give", ArgumentDescription.of("Gives an item-applicable component to players."))
                .argument(MultiplePlayerSelectorArgument.of("targets"), ArgumentDescription.of("The players to give the component to."))
                .argument(ComponentArgument.<CommandSender>newBuilder(plugin, "component")
                        .test(c -> c.baseSystems().containsKey(ItemSystem.ID))
                        .asOptional(), ArgumentDescription.of("The component to give, or the currently held component if not specified."))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1).asOptional(), ArgumentDescription.of("The amount of the component to give."))
                .permission("%s.command.give".formatted(rootName))
                .handler(c -> handle(c, this::give)));

        manager.command(root
                .literal("create", ArgumentDescription.of("Creates a tree of, and gives, an item-applicable component to players, using HOCON syntax."))
                .argument(MultiplePlayerSelectorArgument.of("targets"), ArgumentDescription.of("The players to give the component to."))
                .argument(TreeArgument.<CommandSender>newBuilder(plugin, "node")
                        .test(c -> c.value().baseSystems().containsKey(ItemSystem.ID))
                        .asOptional(), ArgumentDescription.of("The component tree to give, or the currently held component tree if not specified."))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1).asOptional(), ArgumentDescription.of("The amount of the component to give."))
                .permission("%s.command.create".formatted(rootName))
                .handler(c -> handle(c, this::create)));

        manager.command(root
                .literal("build", ArgumentDescription.of("Builds and gives an item-applicable blueprint to players."))
                .argument(MultiplePlayerSelectorArgument.of("targets"), ArgumentDescription.of("The players to give the component to."))
                .argument(BlueprintArgument.<CommandSender>newBuilder(plugin, "blueprint")
                        .test(b -> b.node().value().baseSystems().containsKey(ItemSystem.ID)),
                        ArgumentDescription.of("The blueprint to give."))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1).asOptional(), ArgumentDescription.of("The amount of the component to give."))
                .permission("%s.command.build".formatted(rootName))
                .handler(c -> handle(c, this::build)));

        manager.command(root
                .literal("gui", ArgumentDescription.of("Opens the slot view GUI for a component."))
                .argument(MultiplePlayerSelectorArgument.optional("targets"), ArgumentDescription.of("The players to open the GUI for."))
                .argument(TreeArgument.<CommandSender>newBuilder(plugin, "node")
                        .test(c -> c.value().baseSystems().containsKey(ItemSystem.ID))
                        .asOptional(), ArgumentDescription.of("The component tree to open, or the currently held component if not specified."))
                .flag(CommandFlag.newBuilder("modification")
                        .withAliases("m").withDescription(ArgumentDescription.of("If the component should be able to be modified by modifying slots.")))
                .flag(CommandFlag.newBuilder("limited")
                        .withAliases("l").withDescription(ArgumentDescription.of("If only field-modifiable slots can be modified.")))
                .permission("%s.command.gui".formatted(rootName))
                .handler(c -> handle(c, this::gui)));
    }

    // Command-specific Utils

    private void give(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, Player pSender, PaperTreeNode node) {
        ItemStack item = node.<PaperItemSystem.Instance>system(ItemSystem.ID)
                .orElseThrow().create(locale).handle();
        //noinspection ConstantConditions
        int amount = ctx.getOrDefault("amount", 1);
        List<Player> targets = targets(ctx, "targets", pSender);

        for (Player player : targets) {
            PlayerInventory inventory = player.getInventory();
            for (int i = 0; i < amount; i++) {
                inventory.addItem(item);
            }
        }
        send(sender, locale, "give",
                "amount", ""+amount,
                "component", node.value().name(locale),
                "target", targets.size() == 1
                        ? targets.get(0).displayName()
                        : ""+targets.size());
    }

    // Commands

    private void give(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, Player pSender) {
        give(ctx, sender, locale, pSender, defaultedArg(ctx, "component", pSender,
                () -> plugin.persistenceManager().load(pSender.getInventory().getItemInMainHand())
                        .map(AbstractTreeNode::value)
                        .orElse(null))
                .asTree());
    }

    private void create(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, Player pSender) {
        give(ctx, sender, locale, pSender, defaultedArg(ctx, "node", pSender,
                () -> plugin.persistenceManager().load(pSender.getInventory().getItemInMainHand())
                        .orElse(null)));
    }

    private void build(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, Player pSender) {
        give(ctx, sender, locale, pSender, ctx.<PaperBlueprint>get("blueprint").node());
    }

    private void gui(CommandContext<CommandSender> ctx, CommandSender sender, Locale locale, Player pSender) {
        List<Player> targets = targets(ctx, "targets", pSender);
        PaperTreeNode node = defaultedArg(ctx, "node", pSender,
                () -> plugin.persistenceManager().load(pSender.getInventory().getItemInMainHand())
                        .orElse(null));

        for (Player target : targets) {
            plugin.guis()
                    .create(new SlotViewPane(plugin, 9, 6, target.locale(), node)
                            .modification(ctx.flags().isPresent("modification"))
                            .limited(ctx.flags().isPresent("limited")), evt -> {})
                    .show(target);
        }
    }
}
