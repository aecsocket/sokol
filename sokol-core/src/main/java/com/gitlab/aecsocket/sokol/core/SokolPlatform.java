package com.gitlab.aecsocket.sokol.core;

import com.gitlab.aecsocket.sokol.core.component.Blueprint;
import com.gitlab.aecsocket.sokol.core.component.Component;
import com.gitlab.aecsocket.sokol.core.registry.Registry;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * A platform which provides core functionality.
 */
public interface SokolPlatform {
    /**
     * Gets all registered components.
     * @return The components.
     */
    @NotNull Registry<? extends Component> components();

    /**
     * Gets all registered blueprints.
     * @return The blueprints.
     */
    @NotNull Registry<Blueprint> blueprints();

    /**
     * Gets the default locale that this platform localizes in.
     * @return The default locale.
     */
    @NotNull Locale defaultLocale();

    /**
     * Localizes a string key using arguments.
     * <p>
     * The manner in which the arguments are used is left up to the implementation.
     * @param locale The locale to serialize in. Use {@link #defaultLocale()} if just a default locale needs to be used.
     * @param key The string key.
     * @param args The arguments.
     * @return The component result.
     */
    @NotNull net.kyori.adventure.text.Component localize(Locale locale, String key, Object... args);
}
