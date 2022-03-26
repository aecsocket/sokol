package com.github.aecsocket.sokol.core.impl;

import java.util.*;
import java.util.function.Consumer;

import com.github.aecsocket.minecommons.core.i18n.I18N;
import com.github.aecsocket.minecommons.core.node.MutableAbstractMapNode;
import com.github.aecsocket.sokol.core.*;
import com.github.aecsocket.sokol.core.context.Context;
import com.github.aecsocket.sokol.core.event.NodeEvent;
import com.github.aecsocket.sokol.core.world.ItemStack;

import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class AbstractTreeNode<
    N extends AbstractTreeNode<N, B, C, F, S>,
    B extends BlueprintNode.Scoped<B, N, C, ?>,
    C extends SokolComponent.Scoped<C, ?, ? extends FeatureProfile<?, ? extends FeatureData<?, ? extends F, N>>>,
    F extends FeatureInstance<?, ? extends FeatureData<?, ?, N>, N>,
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
            features.put(entry.getKey(), copy(entry.getValue()));
        }
    }

    protected abstract F copy(F instance);

    private <D extends FeatureData<?, ? extends F, N>> Map<String, F> initFeatures(Map<String, ? extends D> featureData) {
        Map<String, F> result = new HashMap<>();
        for (var entry : value.features().entrySet()) {
            String key = entry.getKey();
            D data = featureData.get(key);
            F instance = (data == null
                ? entry.getValue().setUp()
                : data
            ).asInstance(self());
            result.put(key, instance);
        }
        //for (var entry : featureData.entrySet()) {
        //    result.put(entry.getKey(), entry.getValue().asInstance(self()));
        //}
        return result;
    }

    protected AbstractTreeNode(C value, Map<String, ? extends FeatureData<?, ? extends F, N>> featureData, Context context, @Nullable Tree<N> tree, @Nullable Key<N> key) {
        super(key);
        this.value = value;
        features = initFeatures(featureData);
        this.context = context;
        this.tree = tree;
    }

    protected AbstractTreeNode(C value, Map<String, ? extends FeatureData<?, ? extends F, N>> featureData, Context context, @Nullable Tree<N> tree, N parent, String key) {
        super(parent, key);
        this.value = value;
        features = initFeatures(featureData);
        this.context = context;
        this.tree = tree;
    }

    protected AbstractTreeNode(C value, Map<String, ? extends FeatureData<?, ? extends F, N>> featureData, Context context, N parent, String key) {
        super(parent, key);
        this.value = value;
        features = initFeatures(featureData);
        this.context = context;
    }

    protected AbstractTreeNode(C value, Map<String, ? extends FeatureData<?, ? extends F, N>> featureData, Context context, @Nullable Tree<N> tree) {
        this.value = value;
        features = initFeatures(featureData);
        this.context = context;
        this.tree = tree;
    }

    protected AbstractTreeNode(C value, Map<String, ? extends FeatureData<?, ? extends F, N>> featureData, Context context) {
        this.value = value;
        features = initFeatures(featureData);
        this.context = context;
    }

    public abstract SokolPlatform platform();

    @Override public C value() { return value; }

    @Override public Map<String, F> features() { return new HashMap<>(features); }
    @Override public boolean hasFeature(String key) { return features.containsKey(key); }
    @Override public Set<String> featureKeys() { return features.keySet(); }
    @Override public Optional<F> feature(String key) { return Optional.ofNullable(features.get(key)); }
    @Override public Optional<? extends FeatureData<?, ?, N>> featureData(String key) { return feature(key).map(FeatureInstance::asData); }

    @Override public Context context() { return context; }

    @Override public Tree<N> tree() { return tree; }
    @Override public void tree(Tree<N> tree) { this.tree = tree; }

    @Override
    public N set(String key, N val) {
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
}
