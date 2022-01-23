package com.github.aecsocket.sokol.core.impl;

import com.github.aecsocket.sokol.core.SokolPlatform;
import com.github.aecsocket.sokol.core.api.*;
import com.gitlab.aecsocket.minecommons.core.node.MutableAbstractMapNode;
import com.gitlab.aecsocket.minecommons.core.serializers.Serializers;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Consumer;

public abstract class AbstractBlueprint<
        B extends AbstractBlueprint<B, N, C, D>,
        N extends Node.Scoped<N, B, C, ?, ?>,
        C extends Component.Scoped<C, ?, ? extends FeatureConfig<?, N, C, D>>,
        D extends FeatureData<?, N, B, ?>
> extends MutableAbstractMapNode<B> implements Blueprint.Scoped<B, N, C, D> {
    public static final String ID = "id";
    public static final String CHILDREN = "children";
    public static final String FEATURE_DATA = "feature_data";

    protected final C value;
    protected final Map<String, D> features;

    public AbstractBlueprint(AbstractBlueprint<B, N, C, D> o) {
        super(o);
        value = o.value;
        features = new HashMap<>(o.features);
    }

    public AbstractBlueprint(C value, Map<String, D> features, @Nullable Key<B> key) {
        super(key);
        this.value = value;
        this.features = features;
    }

    public AbstractBlueprint(C value, Map<String, D> features, B parent, String key) {
        super(parent, key);
        this.value = value;
        this.features = features;
    }

    public AbstractBlueprint(C value, Map<String, D> features) {
        this.value = value;
        this.features = features;
    }

    protected abstract SokolPlatform platform();

    @Override public C value() { return value; }

    @Override public Map<String, D> features() { return features; }
    @Override public Optional<D> feature(String key) { return Optional.ofNullable(features.get(key)); }
    @Override public Set<String> featureKeys() { return features.keySet(); }
    @Override public boolean hasFeature(String key) { return features.containsKey(key); }

    @Override
    public B set(String key, B val) throws IncompatibleException {
        value.slot(key)
                .orElseThrow(() -> new IllegalArgumentException("No slot with ID `" + key + "` on component `" + value.id() + "`"))
                .compatible(val, self());
        return super.set(key, val);
    }

    @Override
    public void visitBlueprints(Consumer<Blueprint> visitor) {
        visit(visitor::accept);
    }

    public static abstract class Serializer<
            B extends AbstractBlueprint<B, N, C, D>,
            N extends Node.Scoped<N, B, C, ?, ?>,
            G extends FeatureConfig<?, N, C, D>,
            C extends Component.Scoped<C, ?, G>,
            D extends FeatureData<?, N, B, ?>
    > implements TypeSerializer<B> {
        private final SokolPlatform.Scoped<C, B, ?> platform;

        public Serializer(SokolPlatform.Scoped<C, B, ?> platform) {
            this.platform = platform;
        }

        @Override
        public void serialize(Type type, @Nullable B obj, ConfigurationNode node) throws SerializationException {
            if (obj == null) node.set(null);
            else {
                String id = obj.value.id();
                ConfigurationNode children = node.node(CHILDREN).set(obj.children);
                ConfigurationNode featureData = node.node(FEATURE_DATA);
                for (var entry : obj.features.entrySet()) {
                    entry.getValue().save(featureData.node(entry.getKey()));
                }

                if (children.empty() && featureData.empty())
                    node.set(id);
                else
                    node.node(ID).set(id);
            }
        }

        protected abstract B create(String id, Map<String, D> featureData);

        @Override
        public B deserialize(Type type, ConfigurationNode node) throws SerializationException {
            String id = Serializers.require(node.isMap() ? node.node(ID) : node, String.class);
            C value = platform.components().get(id)
                    .orElseThrow(() -> new SerializationException(node, type, "No component with ID `" + id + "`"));

            if (node.isMap()) {
                Map<String, D> featureData = new HashMap<>();
                // todo feat serializer
                for (var entry : node.node(FEATURE_DATA).childrenMap().entrySet()) {
                    String key = ""+entry.getKey();
                    G config = value.feature(key)
                            .orElseThrow(() -> new SerializationException(entry.getValue(), type, "No feature with ID `" + key + "` exists on component `" + id + "`"));
                    featureData.put(key, config.load(entry.getValue()));
                }

                B root = create(id, featureData);

                for (var entry : node.node(FEATURE_DATA).childrenMap().entrySet()) {
                    String key = ""+entry.getKey();
                    ConfigurationNode config = entry.getValue();
                    NodeSlot slot = value.slot(key)
                            .orElseThrow(() -> new SerializationException(config, type, "No slot `" + key + "` exists on component `" + id + "`"));
                    B child = deserialize(type, config);
                    try {
                        root.set(key, child);
                    } catch (IncompatibleException e) {
                        throw new SerializationException(config, type, "Incompatible node for slot `" + key + "`", e);
                    }
                }

                return root;
            } else {
                return create(id, Collections.emptyMap());
            }
        }
    }
}
