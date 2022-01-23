package com.gitlab.aecsocket.sokol.core;

import com.gitlab.aecsocket.minecommons.core.translation.Localizer;
import com.gitlab.aecsocket.sokol.core.registry.Keyed;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public interface Feature<
        F extends FeatureInstance<D, ?>,
        D
> extends Keyed {
    int PRIORITY_DEFAULT = 0;
    int PRIORITY_LOW = -1000;
    int PRIORITY_HIGH = 1000;

    F load(D data);

    void configure(ConfigurationNode config) throws SerializationException;

    static String renderKey(String id) { return "feature." + id + ".name"; }
    static String renderDescriptionKey(String id) { return "feature." + id + ".description"; }

    @Override
    default Component render(Locale locale, Localizer lc) {
        return lc.safe(locale, renderKey(id()));
    }

    default Component renderDescription(Locale locale, Localizer lc) {
        return lc.safe(locale, renderDescriptionKey(id()));
    }

    Optional<List<Component>> renderConfig(Locale locale, Localizer lc);
}
