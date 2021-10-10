package com.gitlab.aecsocket.sokol.paper.impl;

import com.gitlab.aecsocket.sokol.core.LoadProvider;
import com.gitlab.aecsocket.sokol.core.impl.AbstractComponent;
import com.gitlab.aecsocket.sokol.core.registry.Keyed;
import com.gitlab.aecsocket.sokol.core.registry.ValidationException;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.stat.Stat;
import com.gitlab.aecsocket.sokol.core.stat.StatIntermediate;
import com.gitlab.aecsocket.sokol.paper.FeatureType;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.*;

import static com.gitlab.aecsocket.minecommons.core.serializers.Serializers.*;

public final class PaperComponent extends AbstractComponent<PaperComponent, PaperSlot, PaperFeature<?>, PaperNode> {
    private final SokolPlugin platform;

    public PaperComponent(SokolPlugin platform, String id, Set<String> tags, Map<String, PaperSlot> slots, Map<String, PaperFeature<?>> featureTypes, StatIntermediate stats) {
        super(id, tags, slots, featureTypes, stats);
        this.platform = platform;
    }

    public SokolPlugin platform() { return platform; }

    @Override
    public String toString() {
        return "PaperComponent:" + id + '{' +
                "tags=" + tags +
                ", slots=" + slots +
                ", featureTypes=" + features.keySet() +
                '}';
    }

    public static final class Serializer implements TypeSerializer<PaperComponent> {
        private final SokolPlugin plugin;

        public Serializer(SokolPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public void serialize(Type type, @Nullable PaperComponent obj, ConfigurationNode node) throws SerializationException {
            if (obj == null) node.set(null);
            else {
                throw new UnsupportedOperationException();
            }
        }

        @Override
        public PaperComponent deserialize(Type type, ConfigurationNode node) throws SerializationException {
            String id = Utils.id(type, node);

            List<LoadProvider> loadProviders = new ArrayList<>();

            Map<String, PaperFeature<?>> features = new HashMap<>();
            Map<PaperFeature<?>, ConfigurationNode> featureConfigs = new HashMap<>();
            for (var entry : node.node("features").childrenMap().entrySet()) {
                String featureId = ""+entry.getKey();
                ConfigurationNode config = entry.getValue();
                FeatureType featureType = plugin.featureTypes().get(featureId)
                        .orElseThrow(() -> new SerializationException(node, type, "No feature with ID '" + featureId + "'"));
                PaperFeature<?> feature = featureType.createSystem(config);
                loadProviders.add(featureType);
                features.put(featureId, feature);
                featureConfigs.put(feature, config);
            }

            Map<String, Stat<?>> statTypes = new HashMap<>();
            Map<String, Class<? extends Rule>> ruleTypes = new HashMap<>();
            for (var provider : loadProviders) {
                statTypes.putAll(provider.statTypes().handle());
                String prefix = provider.id();
                for (var ruleType : provider.ruleTypes().entrySet())
                    ruleTypes.put(prefix + ruleType.getKey(), ruleType.getValue());
            }
            plugin.statMapSerializer().types(statTypes);
            plugin.ruleSerializer().types(ruleTypes);

            Map<String, PaperSlot> slots = new HashMap<>();
            for (var entry : node.node("slots").childrenMap().entrySet()) {
                String key = entry.getKey()+"";
                ConfigurationNode value = entry.getValue();
                try {
                    Keyed.validate(key);
                } catch (ValidationException e) {
                    throw new SerializationException(value, type, "Invalid slot key '" + key + "'", e);
                }
                slots.put(key, require(value, PaperSlot.class));
            }

            for (var entry : featureConfigs.entrySet()) {
                entry.getKey().configure(entry.getValue());
            }

            PaperComponent result = new PaperComponent(plugin,
                    id,
                    node.node("tags").get(new TypeToken<Set<String>>(){}, Collections.emptySet()),
                    slots,
                    features,
                    node.node("stats").get(StatIntermediate.class, new StatIntermediate())
            );

            plugin.statMapSerializer().types(null);
            plugin.ruleSerializer().types(null);

            for (var entry : result.slots.entrySet())
                entry.getValue().parent(result, entry.getKey());

            return result;
        }
    }
}
