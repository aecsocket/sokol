package com.gitlab.aecsocket.sokol.core;

import com.gitlab.aecsocket.sokol.core.component.Component;
import com.gitlab.aecsocket.sokol.core.registry.Registry;

import java.util.Locale;

public interface SokolPlatform {
    Registry<? extends Component> components();
    Component component(String id);

    Locale defaultLocale();
    net.kyori.adventure.text.Component localize(Locale locale, String key, Object... args);
}
