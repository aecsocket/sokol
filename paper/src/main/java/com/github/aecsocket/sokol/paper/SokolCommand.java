package com.github.aecsocket.sokol.paper;

import cloud.commandframework.ArgumentDescription;

import com.github.aecsocket.minecommons.paper.plugin.BaseCommand;
import com.github.aecsocket.minecommons.paper.plugin.BasePlugin;

/* package */ final class SokolCommand extends BaseCommand<SokolPlugin> {
    public SokolCommand(SokolPlugin plugin) throws Exception {
        super(plugin, "sokol",
                (mgr, root) -> mgr.commandBuilder(root, ArgumentDescription.of("Plugin main command.")));
    }
}
