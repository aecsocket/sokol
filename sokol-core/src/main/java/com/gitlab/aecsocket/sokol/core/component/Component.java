package com.gitlab.aecsocket.sokol.core.component;

import com.gitlab.aecsocket.sokol.core.registry.Keyed;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;

public interface Component extends Keyed {
    interface Scoped<C extends Scoped<C, S, B>, S extends Slot, B extends System.Base<?>> extends Component {
        @NotNull C self();

        @Override @NotNull Map<String, S> slots();
        @Override @NotNull Map<String, B> baseSystems();
    }

    @NotNull Collection<String> tags();
    boolean tagged(String tag);

    @NotNull Map<String, ? extends Slot> slots();
    @NotNull Map<String, ? extends System.Base<?>> baseSystems();
}
