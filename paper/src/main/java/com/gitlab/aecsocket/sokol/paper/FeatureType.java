package com.gitlab.aecsocket.sokol.paper;

import com.gitlab.aecsocket.minecommons.core.translation.Localizer;
import com.gitlab.aecsocket.sokol.core.LoadProvider;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.stat.StatTypes;
import com.gitlab.aecsocket.sokol.paper.impl.PaperFeature;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Locale;
import java.util.Map;

public interface FeatureType {
    PaperFeature<?> createFeature(SokolPlugin platform, ConfigurationNode config) throws SerializationException;

    interface Keyed extends FeatureType, com.gitlab.aecsocket.sokol.core.registry.Keyed, LoadProvider {
        static String renderKey(String id) { return "feature." + id + ".name"; }

        @Override
        default Component render(Locale locale, Localizer lc) {
            return lc.safe(locale, renderKey(id()));
        }
    }

    static FeatureType.Keyed of(String id, StatTypes statTypes, Map<String, Class<? extends Rule>> ruleTypes, FeatureType factory) {
        return new FeatureTypeImpl(id, statTypes, ruleTypes, factory);
    }
}
