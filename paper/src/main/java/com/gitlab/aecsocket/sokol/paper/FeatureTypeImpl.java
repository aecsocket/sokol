package com.gitlab.aecsocket.sokol.paper;

import com.gitlab.aecsocket.sokol.core.registry.Keyed;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.stat.StatTypes;
import com.gitlab.aecsocket.sokol.paper.impl.PaperFeature;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.Map;
import java.util.function.Function;

/* package */ record FeatureTypeImpl(
        String id,
        StatTypes statTypes,
        Map<String, Class<? extends Rule>> ruleTypes,
        Function<ConfigurationNode, PaperFeature<?>> createSystem
) implements FeatureType {
    FeatureTypeImpl {
        Keyed.validate(id);
    }

    @Override
    public PaperFeature<?> createSystem(ConfigurationNode config) {
        return createSystem.apply(config);
    }
}
