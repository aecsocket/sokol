package com.gitlab.aecsocket.sokol.paper;

import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.stat.StatTypes;
import com.gitlab.aecsocket.sokol.paper.impl.PaperFeature;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Map;

/* package */ record FeatureTypeImpl(
        String id,
        StatTypes statTypes,
        Map<String, Class<? extends Rule>> ruleTypes,
        FeatureType factory
) implements FeatureType.Keyed {
    FeatureTypeImpl {
        com.gitlab.aecsocket.sokol.core.registry.Keyed.validate(id);
    }

    @Override
    public PaperFeature<?> createFeature(SokolPlugin plugin, ConfigurationNode config) throws SerializationException {
        return factory.createFeature(plugin, config);
    }
}
