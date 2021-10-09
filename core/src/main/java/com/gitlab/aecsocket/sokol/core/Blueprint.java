package com.gitlab.aecsocket.sokol.core;

import com.gitlab.aecsocket.minecommons.core.translation.Localizer;
import com.gitlab.aecsocket.sokol.core.registry.Keyed;

import java.util.Locale;

public interface Blueprint<N extends Node> extends Keyed {
    N build();

    @Override
    default net.kyori.adventure.text.Component render(Locale locale, Localizer lc) {
        return lc.safe(locale, "blueprint." + id() + ".name");
    }
}
