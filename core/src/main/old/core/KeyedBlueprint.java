package com.gitlab.aecsocket.sokol.core;

import com.gitlab.aecsocket.minecommons.core.translation.Localizer;
import com.gitlab.aecsocket.sokol.core.registry.Keyed;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public interface KeyedBlueprint extends Blueprint, Keyed {
    static String renderKey(String id) { return "blueprint." + id + ".name"; }
    static String renderDescriptionKey(String id) { return "blueprint." + id + ".description"; }

    @Override
    default Component render(Locale locale, Localizer lc) {
        return lc.safe(locale, renderKey(id()));
    }

    default Optional<List<Component>> renderDescription(Locale locale, Localizer lc) {
        return lc.lines(locale, renderDescriptionKey(id()));
    }
}
