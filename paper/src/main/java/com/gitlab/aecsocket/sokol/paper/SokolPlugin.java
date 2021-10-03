package com.gitlab.aecsocket.sokol.paper;

import com.gitlab.aecsocket.minecommons.paper.plugin.BaseCommand;
import com.gitlab.aecsocket.minecommons.paper.plugin.BasePlugin;
import com.gitlab.aecsocket.sokol.core.SokolPlatform;

public class SokolPlugin extends BasePlugin<SokolPlugin> implements SokolPlatform {
    private final

    @Override
    protected SokolCommand createCommand() throws Exception {
        return new SokolCommand(this);
    }
}
