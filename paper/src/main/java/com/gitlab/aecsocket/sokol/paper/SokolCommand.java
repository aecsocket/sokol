package com.gitlab.aecsocket.sokol.paper;

import cloud.commandframework.ArgumentDescription;
import com.gitlab.aecsocket.minecommons.paper.plugin.BaseCommand;

public class SokolCommand extends BaseCommand<SokolPlugin> {
    public SokolCommand(SokolPlugin plugin) throws Exception {
        super(plugin, "sokol",
                (mgr, root) -> mgr.commandBuilder(root, ArgumentDescription.of("Plugin main command.")));

        /*manager.command(root
                .literal("give", ArgumentDescription.of("Gives an item-applicable component to players."))
                .argument(MultiplePlayerSelectorArgument.of("targets"), ArgumentDescription.of("The players to give the component to."))
                .argument(ComponentArgument.<CommandSender>newBuilder(plugin, "component")
                        .test(c -> c.baseSystems().containsKey(ItemSystem.ID))
                        .asOptional(), ArgumentDescription.of("The component to give, or the currently held component if not specified."))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1).asOptional(), ArgumentDescription.of("The amount of the component to give."))
                .permission("%s.command.give".formatted(rootName))
                .handler(c -> handle(c, this::give)));*/
    }
}
