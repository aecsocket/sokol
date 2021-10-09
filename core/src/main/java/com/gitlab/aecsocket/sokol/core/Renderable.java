package com.gitlab.aecsocket.sokol.core;

import com.gitlab.aecsocket.minecommons.core.translation.Localizer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Locale;

import static net.kyori.adventure.text.format.NamedTextColor.*;

public interface Renderable {
    NamedTextColor CONSTANT = AQUA;
    NamedTextColor OPERATOR = BLUE;
    NamedTextColor BRACKET = GRAY;
    NamedTextColor SYMBOL = WHITE;
    NamedTextColor PATH = DARK_GREEN;

    Component render(Locale locale, Localizer lc);
}
