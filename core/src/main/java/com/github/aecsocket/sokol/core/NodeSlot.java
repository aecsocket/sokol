package com.github.aecsocket.sokol.core;

import com.github.aecsocket.minecommons.core.i18n.I18N;
import com.github.aecsocket.minecommons.core.i18n.Renderable;
import net.kyori.adventure.text.Component;

import java.util.Locale;
import java.util.Set;

public interface NodeSlot extends Renderable {
    String
        I18N_KEY = "slot",
        REQUIRED = "required",
        MODIFIABLE = "modifiable";

    SokolComponent parent();
    String key();

    Set<String> tags();
    boolean tagged(String key);

    boolean required();
    boolean modifiable();

    <N extends SokolNode> void compatible(N target, N parent) throws IncompatibleException;

    @Override
    default Component render(I18N i18n, Locale locale) {
        return i18n.line(locale, I18N_KEY + "." + key());
    }

    interface Scoped<
        S extends Scoped<S, C>,
        C extends SokolComponent
    > extends NodeSlot {
        @Override C parent();
    }
}
