package com.gitlab.aecsocket.sokol.paper;

import com.gitlab.aecsocket.minecommons.paper.plugin.BasePlugin;
import com.gitlab.aecsocket.sokol.core.Blueprint;
import com.gitlab.aecsocket.sokol.core.Component;
import com.gitlab.aecsocket.sokol.core.SokolPlatform;
import com.gitlab.aecsocket.sokol.core.registry.Registry;

import java.util.Locale;

public class SokolPlugin extends BasePlugin<SokolPlugin> implements SokolPlatform {
    //private final


    @Override
    public Registry<? extends Component> components() {
        return null;
    }

    @Override
    public Registry<? extends Blueprint<?>> blueprints() {
        return null;
    }

    @Override
    public SokolCommand createCommand() throws Exception {
        return new SokolCommand(this);
    }
}
