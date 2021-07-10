package com.gitlab.aecsocket.sokol.core;

import com.gitlab.aecsocket.minecommons.core.translation.Localizer;
import com.gitlab.aecsocket.sokol.core.component.Blueprint;
import com.gitlab.aecsocket.sokol.core.component.Component;
import com.gitlab.aecsocket.sokol.core.registry.Registry;

import java.util.Locale;

/**
 * A platform which provides core functionality.
 */
public interface SokolPlatform {
    /**
     * Gets all registered components.
     * @return The components.
     */
    Registry<? extends Component> components();

    /**
     * Gets all registered blueprints.
     * @return The blueprints.
     */
    Registry<? extends Blueprint<?>> blueprints();

    /**
     * Gets the default locale that this platform localizes in.
     * @return The default locale.
     */
    Locale defaultLocale();

    /**
     * Gets the localizer used to localize strings.
     * @return The localizer.
     */
    Localizer localizer();

    /**
     * Localizes a string key using arguments.
     * <p>
     * The manner in which the arguments are used is left up to the implementation.
     * @param locale The locale to serialize in. Use {@link #defaultLocale()} if just a default locale needs to be used.
     * @param key The string key.
     * @param args The arguments.
     * @return The component result.
     */
    net.kyori.adventure.text.Component localize(Locale locale, String key, Object... args);
}
