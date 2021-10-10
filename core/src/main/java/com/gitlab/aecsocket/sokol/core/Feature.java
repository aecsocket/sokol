package com.gitlab.aecsocket.sokol.core;

import com.gitlab.aecsocket.minecommons.core.translation.Localizer;
import com.gitlab.aecsocket.sokol.core.registry.Keyed;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public interface Feature<
        F extends FeatureInstance<N>,
        N extends Node.Scoped<N, ?, ?>
> extends Keyed {
    F create(N node);

    void configure(ConfigurationNode config);

    @Override
    default Component render(Locale locale, Localizer lc) {
        return lc.safe(locale, "feature." + id() + ".name");
    }

    default Component renderDescription(Locale locale, Localizer lc) {
        return lc.safe(locale, "feature." + id() + ".description");
    }

    Optional<List<Component>> renderConfig(Locale locale, Localizer lc);
}
