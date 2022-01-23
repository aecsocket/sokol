package com.github.aecsocket.sokol.core.impl;

import com.github.aecsocket.sokol.core.SokolPlatform;
import com.github.aecsocket.sokol.core.api.Component;
import com.github.aecsocket.sokol.core.api.Feature;
import com.github.aecsocket.sokol.core.api.FeatureConfig;
import com.github.aecsocket.sokol.core.api.NodeSlot;
import com.github.aecsocket.sokol.core.rule.node.NodeRule;
import com.github.aecsocket.sokol.core.stat.Stat;
import com.github.aecsocket.sokol.core.stat.StatIntermediate;
import com.gitlab.aecsocket.minecommons.core.i18n.I18N;
import com.gitlab.aecsocket.minecommons.core.serializers.Serializers;

import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.*;

public abstract class AbstractComponent<
        C extends AbstractComponent<C, S, G>,
        S extends NodeSlot,
        G extends FeatureConfig<?, ?, C, ?>
> implements Component.Scoped<C, S, G> {
    public static final String I18N_KEY = "component";
    public static final String TAGS = "tags";
    public static final String SLOTS = "slots";
    public static final String FEATURES = "features";
    public static final String SOFT_FEATURES = "soft_features";
    public static final String STATS = "stats";

    protected final String id;
    protected final Set<String> tags;
    protected final Map<String, S> slots;
    protected final Map<String, G> featureConfigs;
    protected final StatIntermediate stats;

    public AbstractComponent(String id, Set<String> tags, Map<String, S> slots, Map<String, G> featureConfigs, StatIntermediate stats) {
        this.id = id;
        this.tags = tags;
        this.slots = slots;
        this.featureConfigs = featureConfigs;
        this.stats = stats;
    }

    protected abstract SokolPlatform platform();

    @Override public String id() { return id; }

    @Override public Set<String> tags() { return tags; }
    @Override public boolean tagged(String tag) { return tags.contains(tag); }

    @Override public Map<String, S> slots() { return slots; }
    @Override public Optional<S> slot(String key) { return Optional.ofNullable(slots.get(key)); }

    @Override public Map<String, G> features() { return featureConfigs; }
    @Override public Optional<G> feature(String key) { return Optional.ofNullable(featureConfigs.get(key)); }

    @Override public StatIntermediate stats() { return stats; }

    @Override
    public net.kyori.adventure.text.Component render(I18N i18n, Locale locale) {
        return i18n.line(locale, I18N_KEY + "." + id + "." + NAME);
    }

    @Override
    public Optional<List<net.kyori.adventure.text.Component>> renderDescription(I18N i18n, Locale locale) {
        return i18n.orLines(locale, I18N_KEY + "." + id + "." + DESCRIPTION);
    }

    public static abstract class Serializer<
            C extends AbstractComponent<C, S, G>,
            S extends NodeSlot,
            F extends Feature<G>,
            G extends FeatureConfig<?, ?, C, ?>
    > implements TypeSerializer<C> {
        private final SokolPlatform.Scoped<C, ?, F> platform;

        public Serializer(SokolPlatform.Scoped<C, ?, F> platform) {
            this.platform = platform;
        }

        @Override
        public void serialize(Type type, @Nullable C obj, ConfigurationNode node) throws SerializationException {
            throw new UnsupportedOperationException();
        }

        protected abstract TypeToken<Map<String, S>> typeSlots();
        protected abstract TypeToken<Map<String, G>> typeFeatures();

        protected abstract C create(String id, Set<String> tags, Map<String, S> slots, Map<String, G> featureConfigs, StatIntermediate stats);

        @Override
        public C deserialize(Type type, ConfigurationNode node) throws SerializationException {
            List<F> providerFeatures = new ArrayList<>();
            Map<F, ConfigurationNode> hardFeatures = new HashMap<>();
            for (var entry : node.node(FEATURES).childrenMap().entrySet()) {
                String id = ""+entry.getKey();
                ConfigurationNode config = entry.getValue();
                F feature = platform.features().get(id)
                        .orElseThrow(() -> new SerializationException(config, type, "No feature with ID `" + id + "`"));
                providerFeatures.add(feature);
                hardFeatures.put(feature, config);
            }

            for (var child : node.node(SOFT_FEATURES).childrenList()) {
                String id = Serializers.require(child, String.class);
                providerFeatures.add(platform.features().get(id)
                        .orElseThrow(() -> new SerializationException(child, type, "No feature with ID `" + id + "`")));
            }

            Map<String, Stat<?>> statTypes = new HashMap<>();
            Map<String, Class<? extends NodeRule>> ruleTypes = new HashMap<>();
            for (var feature : providerFeatures) {
                statTypes.putAll(feature.statTypes().handle());
                ruleTypes.putAll(feature.ruleTypes().handle());
            }

            platform.setUpSerializers(statTypes, ruleTypes);

            Map<String, G> features = new HashMap<>();
            for (var entry : hardFeatures.entrySet()) {
                F feature = entry.getKey();
                ConfigurationNode config = entry.getValue();
                String id = feature.id();
                try {
                    features.put(id, feature.configure(config));
                } catch (SerializationException e) {
                    throw new SerializationException(config, type, "Could not configure feature `" + id + "`", e);
                }
            }

            C result = create(
                    ""+Objects.requireNonNull(node.key()),
                    node.node(TAGS).get(new TypeToken<Set<String>>() {}, Collections.emptySet()),
                    node.node(SLOTS).get(typeSlots(), Collections.emptyMap()),
                    features,
                    node.node(STATS).get(StatIntermediate.class, new StatIntermediate())
            );

            platform.tearDownSerializers();

            return result;
        }
    }
}
