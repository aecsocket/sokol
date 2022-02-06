package com.github.aecsocket.sokol.core.impl;

import java.lang.reflect.Type;
import java.util.*;

import com.github.aecsocket.minecommons.core.i18n.I18N;
import com.github.aecsocket.minecommons.core.serializers.Serializers;
import com.github.aecsocket.sokol.core.*;
import com.github.aecsocket.sokol.core.rule.Rule;
import com.github.aecsocket.sokol.core.stat.Stat;
import com.github.aecsocket.sokol.core.stat.StatIntermediate;

import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

public abstract class AbstractComponent<
    C extends AbstractComponent<C, S, P>,
    S extends NodeSlot.Scoped<S, C>,
    P extends FeatureProfile<P, ?, ?>
> implements SokolComponent.Scoped<C, S, P> {
    public static final String I18N_KEY = "component";
    public static final String TAGS = "tags";
    public static final String SLOTS = "slots";
    public static final String FEATURES = "features";
    public static final String SOFT_FEATURES = "soft_features";
    public static final String STATS = "stats";

    protected final String id;
    protected final Set<String> tags;
    protected final Map<String, P> features;
    protected final Map<String, S> slots;
    protected final StatIntermediate stats;

    protected AbstractComponent(String id, Set<String> tags, Map<String, P> features, Map<String, S> slots, StatIntermediate stats) {
        this.id = id;
        this.tags = Collections.unmodifiableSet(tags);
        this.features = Collections.unmodifiableMap(features);
        this.slots = Collections.unmodifiableMap(slots);
        this.stats = stats;
    }

    public abstract SokolPlatform platform();

    @Override public String id() { return id; }

    @Override public Set<String> tags() { return new HashSet<>(tags); }
    @Override public boolean tagged(String key) { return tags.contains(key); }

    @Override public Map<String, P> features() { return new HashMap<>(features); }
    @Override public Optional<P> feature(String key) { return Optional.ofNullable(features.get(key)); }

    @Override public Map<String, S> slots() { return new HashMap<>(slots); }
    @Override public Optional<S> slot(String key) { return Optional.ofNullable(slots.get(key)); }

    @Override public StatIntermediate stats() { return stats; }

    @Override
    public Component render(I18N i18n, Locale locale) {
        return i18n.line(locale, I18N_KEY + "." + id + "." + NAME);
    }

    @Override
    public Optional<List<Component>> renderDescription(I18N i18n, Locale locale) {
        return i18n.orLines(locale, I18N_KEY + "." + id + "." + DESCRIPTION);
    }

    public abstract static class Serializer<
        C extends AbstractComponent<C, S, P>,
        S extends BasicNodeSlot<S, C>,
        F extends Feature<F, P>,
        P extends FeatureProfile<P, F, ?>
    > implements TypeSerializer<C> {
        private final SokolPlatform.Scoped<C, F> platform;

        public Serializer(SokolPlatform.Scoped<C, F> platform) {
            this.platform = platform;
        }

        @Override
        public void serialize(Type type, @Nullable C obj, ConfigurationNode node) throws SerializationException {
            throw new UnsupportedOperationException();
        }

        protected abstract Class<S> slotType();

        protected abstract C create(
            String id, Set<String> tags, Map<String, S> slots, Map<String, P> features, StatIntermediate stats
        );

        @Override
        public C deserialize(Type type, ConfigurationNode node) throws SerializationException {
            List<F> providers = new ArrayList<>();
            Map<F, ConfigurationNode> featureConfigs = new HashMap<>();
            for (var entry : node.node(FEATURES).childrenMap().entrySet()) {
                String id = ""+entry.getKey();
                ConfigurationNode config = entry.getValue();
                F feature = platform.features().get(id)
                    .orElseThrow(() -> new SerializationException(config, type, "No feature with ID `" + id + "`"));
                providers.add(feature);
                featureConfigs.put(feature, config);
            }

            for (var child : node.node(SOFT_FEATURES).childrenList()) {
                String id = Serializers.require(child, String.class);
                providers.add(platform.features().get(id)
                    .orElseThrow(() -> new SerializationException(child, type, "No feature with ID `" + id + "`")));
            }

            Map<String, Stat<?>> statTypes = new HashMap<>();
            Map<String, Class<? extends Rule>> ruleTypes = new HashMap<>();
            for (var provider : providers) {
                statTypes.putAll(provider.statTypes().map());
                String id = provider.id();
                for (var entry : provider.ruleTypes().map().entrySet()) {
                    ruleTypes.put(id + ":" + entry.getKey(), entry.getValue());
                }
            }

            platform.setUpSerializers(statTypes, ruleTypes);

            Map<String, P> features = new HashMap<>();
            for (var entry : featureConfigs.entrySet()) {
                F feature = entry.getKey();
                ConfigurationNode config = entry.getValue();
                String id = feature.id();
                try {
                    features.put(id, feature.setUp(config));
                } catch (SerializationException e) {
                    throw new SerializationException(config, type, "Could not configure feature `" + id + "`", e);
                }
            }

            Map<String, S> slots = new HashMap<>();
            for (var entry : node.node(SLOTS).childrenMap().entrySet()) {
                ConfigurationNode config = entry.getValue();
                slots.put(SokolPlatform.deserializeId(slotType(), config), config.get(slotType()));
            }

            C result = create(
                SokolPlatform.deserializeId(type, node),
                node.node(TAGS).get(new TypeToken<Set<String>>() {}, Collections.emptySet()),
                slots,
                features,
                node.node(STATS).get(StatIntermediate.class, new StatIntermediate())
            );

            for (var entry : slots.entrySet()) {
                String key = entry.getKey();
                entry.getValue().setUp(result, entry.getKey());
            }

            platform.tearDownSerializers();
            return result;
        }
    }
}
