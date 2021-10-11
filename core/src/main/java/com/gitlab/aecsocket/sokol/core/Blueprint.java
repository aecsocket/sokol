package com.gitlab.aecsocket.sokol.core;

import com.gitlab.aecsocket.minecommons.core.translation.Localizer;
import com.gitlab.aecsocket.sokol.core.registry.Keyed;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public interface Blueprint<N extends Node> extends Keyed {
    N create();

    @Override
    default net.kyori.adventure.text.Component render(Locale locale, Localizer lc) {
        return lc.safe(locale, "blueprint." + id() + ".name");
    }

    default Optional<List<Component>> renderDescription(Locale locale, Localizer lc) {
        return lc.lines(locale, "blueprint." + id() + ".description");
    }
}
