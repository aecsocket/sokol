package com.github.aecsocket.sokol.core.impl;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import com.github.aecsocket.minecommons.core.i18n.I18N;
import com.github.aecsocket.minecommons.core.node.MutableAbstractMapNode;
import com.github.aecsocket.minecommons.core.serializers.Serializers;
import com.github.aecsocket.sokol.core.*;
import com.github.aecsocket.sokol.core.context.Context;
import com.github.aecsocket.sokol.core.event.NodeEvent;
import com.github.aecsocket.sokol.core.world.ItemStack;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

public abstract class AbstractTreeNode<
    N extends AbstractTreeNode<N, B, C, F, S>,
    B extends BlueprintNode.Scoped<B, N, C, ? extends FeatureData<?, ?, F, N>>,
    C extends SokolComponent.Scoped<C, ?, ? extends FeatureProfile<?, ?, ? extends FeatureData<?, ?, F, N>>>,
    F extends FeatureInstance<F, ? extends FeatureData<?, ?, F, N>, N>,
    S extends ItemStack.Scoped<S, B>
> extends MutableAbstractMapNode<N> implements TreeNode.Scoped<N, B, C, F, S> {
    public static final String ID = "id";
    public static final String FEATURES = "features";
    public static final String CHILDREN = "children";

    protected final C value;
    protected final Map<String, F> features;
    protected final Context context;
    protected Tree<N> tree;

    protected AbstractTreeNode(AbstractTreeNode<N, B, C, F, S> o) {
        super(o);
        value = o.value;
        context = o.context;
        tree = o.tree;

        features = new HashMap<>();
        for (var entry : o.features.entrySet()) {
            features.put(entry.getKey(), entry.getValue().copy());
        }
    }

    protected AbstractTreeNode(C value, Map<String, ? extends FeatureData<?, ?, F, N>> featureData, Context context, @Nullable Tree<N> tree, @Nullable Key<N> key) {
        super(key);
        this.value = value;
        features = buildFeatures(featureData);
        this.context = context;
        this.tree = tree;
    }

    protected AbstractTreeNode(C value, Map<String, ? extends FeatureData<?, ?, F, N>> featureData, Context context, @Nullable Tree<N> tree, N parent, String key) {
        super(parent, key);
        this.value = value;
        features = buildFeatures(featureData);
        this.context = context;
        this.tree = tree;
    }

    protected AbstractTreeNode(C value, Map<String, ? extends FeatureData<?, ?, F, N>> featureData, Context context, N parent, String key) {
        super(parent, key);
        this.value = value;
        features = buildFeatures(featureData);
        this.context = context;
    }

    protected AbstractTreeNode(C value, Map<String, ? extends FeatureData<?, ?, F, N>> featureData, Context context, @Nullable Tree<N> tree) {
        this.value = value;
        features = buildFeatures(featureData);
        this.context = context;
        this.tree = tree;
    }

    protected AbstractTreeNode(C value, Map<String, ? extends FeatureData<?, ?, F, N>> featureData, Context context) {
        this.value = value;
        features = buildFeatures(featureData);
        this.context = context;
    }

    private <D extends FeatureData<?, ?, F, N>> Map<String, F> buildFeatures(Map<String, D> featureData) {
        Map<String, F> result = new HashMap<>();
        for (var entry : value.features().entrySet()) {
            String key = entry.getKey();
            D data = featureData.get(key);
            result.put(key, (data == null
                ? entry.getValue().setUp()
                : data)
                .asInstance(self())
            );
        }
        for (var entry : featureData.entrySet()) {
            result.put(entry.getKey(), entry.getValue().asInstance(self()));
        }
        return result;
    }

    public abstract SokolPlatform platform();

    @Override public C value() { return value; }

    @Override public Map<String, F> features() { return new HashMap<>(features); }
    @Override public boolean hasFeature(String key) { return features.containsKey(key); }
    @Override public Optional<F> feature(String key) { return Optional.ofNullable(features.get(key)); }
    @Override public Optional<? extends FeatureData<?, ?, F, N>> featureData(String key) { return feature(key).map(FeatureInstance::asData); }

    @Override public Context context() { return context; }

    @Override public Tree<N> tree() { return tree;}
    @Override public void tree(Tree<N> tree) { this.tree = tree;}

    @Override
    public N set(String key, N val) {
        value.slot(key)
            .orElseThrow(() -> new IllegalArgumentException("No slot with ID `" + key + "` on component `" + value.id() + "`"))
            .compatible(val, self());
        return super.set(key, val);
    }

    @Override
    public void visitComponents(Consumer<TreeNode> visitor) {
        visit(visitor::accept);
    }

    protected abstract S createItem();
    protected abstract NodeEvent.CreateItem<N, B, S> createItemEvent(S item);

    @Override
    public S asItem() {
        S item = createItem();
        I18N i18n = platform().i18n();
        Locale locale = context.locale();

        item.name(value.render(i18n, locale));
        tree.events().call(createItemEvent(item));

        return item;
    }

    @Override
    public N build() {
        tree = Tree.build(self());
        return self();
    }

    @Override
    public boolean complete() {
        return tree.complete();
    }

    public abstract static class Serializer<
        N extends AbstractTreeNode<N, B, C, I, S>,
        B extends BlueprintNode.Scoped<B, N, C, ? extends FeatureData<?, ?, I, N>>,
        C extends SokolComponent.Scoped<C, ?, P>,
        F extends Feature<F, P>,
        P extends FeatureProfile<P, F, D>,
        D extends FeatureData<D, P, I, N>,
        I extends FeatureInstance<I, D, N>,
        S extends ItemStack.Scoped<S, B>
    > implements TypeSerializer<N> {
        private final SokolPlatform.Scoped<C, F> platform;

        public Serializer(SokolPlatform.Scoped<C, F> platform) {
            this.platform = platform;
        }

        protected abstract Class<N> nodeType();

        protected abstract N create(C value, Map<String, D> featureData);

        @Override
        public void serialize(Type type, @Nullable N obj, ConfigurationNode node) throws SerializationException {
            if (obj == null) node.set(null);
            else {
                ConfigurationNode features = node.node(FEATURES);
                for (var entry : obj.features.entrySet()) {
                    ConfigurationNode config = features.node(entry.getKey());
                    entry.getValue().asData().save(config);
                }

                ConfigurationNode children = node.node(CHILDREN);
                for (var entry : obj.children.entrySet()) {
                    children.node(entry.getKey()).set(entry.getValue());
                }

                (features.empty() && children.empty() ? node : node.node(ID)).set(obj.value.id());
            }
        }

        @Override
        public N deserialize(Type type, ConfigurationNode node) throws SerializationException {
            String id = Serializers.require(node.isMap() ? node.node(ID) : node, String.class);
            C value = platform.components().get(id)
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

            N root = create(value, featureData);

            for (var entry : node.node(CHILDREN).childrenMap().entrySet()) {
                root.setUnsafe(""+entry.getKey(), entry.getValue().get(nodeType()));
            }

            return root;
        }
    }
}
