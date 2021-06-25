package com.gitlab.aecsocket.sokol.core.component;

import com.gitlab.aecsocket.sokol.core.SokolPlatform;
import com.gitlab.aecsocket.sokol.core.registry.Keyed;
import com.gitlab.aecsocket.sokol.core.stat.StatLists;
import com.gitlab.aecsocket.sokol.core.system.System;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;

public interface Component extends Keyed {
    interface Scoped<C extends Scoped<C, S, B>, S extends Slot, B extends System> extends Component {
        @NotNull C self();

        @Override @NotNull Map<String, S> slots();
        @Override S slot(String key);

        @Override @NotNull Map<String, B> baseSystems();
        @Override B baseSystem(String id);
    }

    @NotNull SokolPlatform platform();

    default @NotNull net.kyori.adventure.text.Component name(Locale locale) {
        return platform().localize(locale, "component." + id());
    }

    @NotNull Collection<String> tags();
    boolean tagged(String tag);

    @NotNull Map<String, ? extends Slot> slots();
    Slot slot(String key);

    @NotNull Map<String, ? extends System> baseSystems();
    System baseSystem(String id);

    @NotNull StatLists stats();
}
