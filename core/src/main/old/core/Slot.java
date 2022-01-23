package com.gitlab.aecsocket.sokol.core;

import com.gitlab.aecsocket.minecommons.core.translation.Localizer;
import com.gitlab.aecsocket.sokol.core.node.IncompatibilityException;

import java.util.Locale;
import java.util.Set;

public interface Slot extends Renderable {
    Component parent();
    String key();

    Set<String> tags();
    boolean tagged(String tag);

    <N extends Node.Scoped<N, ?, ?, ?, ?>> void compatibility(N parent, N child) throws IncompatibilityException;

    static String renderKey(String key) { return "slot." + key + ".name"; }

    @Override
    default net.kyori.adventure.text.Component render(Locale locale, Localizer lc) {
        return lc.safe(locale, renderKey(key()));
    }
}
