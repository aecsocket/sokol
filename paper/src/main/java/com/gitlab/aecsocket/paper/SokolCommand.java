package com.gitlab.aecsocket.paper;

import cloud.commandframework.ArgumentDescription;
import com.gitlab.aecsocket.minecommons.paper.plugin.BaseCommand;

/* package */ final class SokolCommand extends BaseCommand<SokolPlugin> {
    public SokolCommand(SokolPlugin plugin) throws Exception {
        super(plugin, "sokol",
                (mgr, root) -> mgr.commandBuilder(root, ArgumentDescription.of("Plugin main command.")));
    }
}
