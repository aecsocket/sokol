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
    Localizer lc();
}
