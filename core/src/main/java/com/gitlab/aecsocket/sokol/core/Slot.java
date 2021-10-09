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

    void compatibility(Node parent, Node child) throws IncompatibilityException;

    @Override
    default net.kyori.adventure.text.Component render(Locale locale, Localizer lc) {
        return lc.safe(locale, "slot.name." + key());
    }
}
