package com.github.aecsocket.sokol.core.impl;

import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Consumer;

import com.github.aecsocket.minecommons.core.node.MutableAbstractMapNode;
import com.github.aecsocket.minecommons.core.serializers.Serializers;
import com.github.aecsocket.sokol.core.*;
import com.github.aecsocket.sokol.core.world.ItemStack;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

public abstract class AbstractBlueprintNode<
    B extends AbstractBlueprintNode<B, N, C, F>,
    N extends TreeNode.Scoped<N, B, C, ?, ? extends ItemStack.Scoped<?, B>>,
    C extends SokolComponent.Scoped<C, ?, ?>,
    F extends FeatureData<?, ?, N>
> extends MutableAbstractMapNode<B> implements BlueprintNode.Scoped<B, N, C, F> {
    public static final String ID = "id";
    public static final String FEATURES = "features";
    public static final String CHILDREN = "children";

    protected final C value;
    protected final Map<String, F> featureData;

    protected AbstractBlueprintNode(AbstractBlueprintNode<B, N, C, F> o) {
        super(o);
        value = o.value;
        featureData = new HashMap<>(o.featureData);
    }

    protected AbstractBlueprintNode(C value, Map<String, F> featureData, @Nullable Key<B> key) {
        super(key);
        this.value = value;
        this.featureData = featureData;
    }

    protected AbstractBlueprintNode(C value, Map<String, F> featureData, B parent, String key) {
        super(parent, key);
        this.value = value;
        this.featureData = featureData;
    }

    protected AbstractBlueprintNode(C value, Map<String, F> featureData) {
        this.value = value;
        this.featureData = featureData;
    }

    public AbstractBlueprintNode(C value) {
        this.value = value;
        featureData = Collections.emptyMap();
    }

    @Override public C value() { return value; }
    
    @Override public Map<String, F> featureData() { return new HashMap<>(featureData); }
    @Override public boolean hasFeature(String key) { return featureData.containsKey(key); }
    @Override public Set<String> featureKeys() { return featureData.keySet(); }
    @Override public Optional<F> featureData(String key) { return Optional.ofNullable(featureData.get(key)); }

    @Override
    public B set(String key, B val) {
        value.slot(key)
            .orElseThrow(() -> new IllegalArgumentException("No slot with ID `" + key + "` on component `" + value.id() + "`"))
            .compatible(val, self());
        return super.set(key, val);
    }

    @Override
    public void visitSokolNodes(Consumer<SokolNode> visitor) {
        visit(visitor::accept);
    }

    @Override
    public void visitBlueprints(Consumer<BlueprintNode> visitor) {
        visit(visitor::accept);
    }

    @Override
    public boolean complete() {
        return SokolNode.complete(this);
    }

    public abstract static class Serializer<
        B extends AbstractBlueprintNode<B, N, C, D>,
        N extends TreeNode.Scoped<N, B, C, I, ? extends ItemStack.Scoped<?, B>>,
        C extends SokolComponent.Scoped<C, ?, P>,
        F extends Feature<?>,
        P extends FeatureProfile<?, ? extends D>,
        D extends FeatureData<?, ?, N>,
        I extends FeatureInstance<?, N>
    > implements TypeSerializer<B> {
        protected abstract SokolPlatform.Scoped<F, C, ?> platform();
        protected abstract B create(C value, Map<String, D> featureData, @Nullable B parent, @Nullable String key);

        @Override
        public void serialize(Type type, @Nullable B obj, ConfigurationNode node) throws SerializationException {
            if (obj == null) node.set(null);
            else {
                ConfigurationNode features = node.node(FEATURES);
                for (var entry : obj.featureData.entrySet()) {
                    ConfigurationNode config = features.node(entry.getKey());
                    entry.getValue().save(config);
                }

                ConfigurationNode children = node.node(CHILDREN);
                for (var entry : obj.children.entrySet()) {
                    children.node(entry.getKey()).set(entry.getValue());
                }

                (features.empty() && children.empty() ? node : node.node(ID)).set(obj.value.id());
            }
        }

        protected B deserialize(Type type, ConfigurationNode node, @Nullable B parent, @Nullable String key) throws SerializationException {
            String id = Serializers.require(node.isMap() ? node.node(ID) : node, String.class);
            C value = platform().components().get(id)
                    .orElseThrow(() -> new SerializationException(node, type, "No component with ID `" + id + "`"));

            Map<String, D> featureData = new HashMap<>();
            for (var entry : node.node(FEATURES).childrenMap().entrySet()) {
                ConfigurationNode config = entry.getValue();
                String featureId = ""+entry.getKey();
                P profile = value.features().get(featureId);
                if (profile == null)
                    throw new SerializationException(config, type, "No feature with ID `" + featureId + "` on component `" + id + "`");
                try {
                    featureData.put(featureId, profile.load(config));
                } catch (SerializationException e) {
                    throw new SerializationException(config, type, "Could not load feature instance for `" + featureId + "` on component `" + id + "`", e);
                }
            }

            B root = create(value, featureData, parent, key);

            for (var entry : node.node(CHILDREN).childrenMap().entrySet()) {
                String childKey = ""+entry.getKey();
                root.setUnsafe(childKey, deserialize(type, entry.getValue(), root, childKey));
            }

            return root;
        }

        @Override
        public B deserialize(Type type, ConfigurationNode node) throws SerializationException {
            return deserialize(type, node, null, null);
        }
    }
}
