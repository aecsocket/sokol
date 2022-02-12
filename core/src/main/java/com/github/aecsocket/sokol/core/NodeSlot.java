package com.github.aecsocket.sokol.core;

import com.github.aecsocket.minecommons.core.i18n.I18N;
import com.github.aecsocket.minecommons.core.i18n.Renderable;
import net.kyori.adventure.text.Component;

import java.util.Locale;
import java.util.Set;

public interface NodeSlot extends Renderable {
    String REQUIRED = "required";
    String NODE_SLOT = "node_slot";

    SokolComponent parent();
    String key();

    Set<String> tags();
    boolean tagged(String key);

    boolean required();

    <N extends SokolNode> void compatible(N target, N parent) throws IncompatibleException;

    @Override
    default Component render(I18N i18n, Locale locale) {
        return i18n.line(locale, NODE_SLOT + "." + key());
    }

    interface Scoped<
        S extends Scoped<S, C>,
        C extends SokolComponent
    > extends NodeSlot {
        @Override C parent();
    }
}
