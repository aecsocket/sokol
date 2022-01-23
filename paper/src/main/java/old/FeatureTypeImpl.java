package old;

import old.impl.PaperFeature;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Map;

import com.github.aecsocket.sokol.core.rule.node.NodeRule;
import com.github.aecsocket.sokol.core.stat.StatTypes;

/* package */ record FeatureTypeImpl(
        String id,
        StatTypes statTypes,
        Map<String, Class<? extends NodeRule>> ruleTypes,
        FeatureType factory
) implements FeatureType.Keyed {
    FeatureTypeImpl {
        com.github.aecsocket.sokol.core.registry.Keyed.validate(id);
    }

    @Override
    public PaperFeature<?> createFeature(SokolPlugin plugin, ConfigurationNode config) throws SerializationException {
        return factory.createFeature(plugin, config);
    }
}
