package com.gitlab.aecsocket.sokol.paper;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.standard.StringArgument;
import com.gitlab.aecsocket.minecommons.paper.plugin.BaseCommand;
import com.gitlab.aecsocket.sokol.core.component.Component;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

/* package */ class SokolCommand extends BaseCommand<SokolPlugin> {
    public SokolCommand(SokolPlugin plugin) throws Exception {
        super(plugin, "sokol",
                (mgr, root) -> mgr.commandBuilder(root, ArgumentDescription.of("Plugin main command.")));
    }
}
