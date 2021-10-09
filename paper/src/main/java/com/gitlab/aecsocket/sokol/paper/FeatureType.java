package com.gitlab.aecsocket.sokol.paper;

import com.gitlab.aecsocket.minecommons.core.translation.Localizer;
import com.gitlab.aecsocket.sokol.core.LoadProvider;
import com.gitlab.aecsocket.sokol.core.registry.Keyed;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.stat.StatTypes;
import com.gitlab.aecsocket.sokol.paper.impl.PaperFeature;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public interface FeatureType extends Keyed, LoadProvider {
    PaperFeature<?> createSystem(ConfigurationNode config);

    @Override
    default Component render(Locale locale, Localizer lc) {
        return lc.safe(locale, "feature.name." + id());
    }

    static FeatureType of(String id, StatTypes statTypes, Map<String, Class<? extends Rule>> ruleTypes, Function<ConfigurationNode, PaperFeature<?>> createSystem) {
        return new FeatureTypeImpl(id, statTypes, ruleTypes, createSystem);
    }
}
