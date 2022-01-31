package com.github.aecsocket.sokol.core.impl;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import com.github.aecsocket.minecommons.core.i18n.I18N;
import com.github.aecsocket.minecommons.core.node.MutableAbstractMapNode;
import com.github.aecsocket.sokol.core.BlueprintNode;
import com.github.aecsocket.sokol.core.FeatureData;
import com.github.aecsocket.sokol.core.FeatureInstance;
import com.github.aecsocket.sokol.core.FeatureProfile;
import com.github.aecsocket.sokol.core.SokolComponent;
import com.github.aecsocket.sokol.core.SokolPlatform;
import com.github.aecsocket.sokol.core.Tree;
import com.github.aecsocket.sokol.core.TreeNode;
import com.github.aecsocket.sokol.core.context.Context;
import com.github.aecsocket.sokol.core.event.NodeEvent;
import com.github.aecsocket.sokol.core.world.ItemStack;

import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class AbstractTreeNode<
    N extends AbstractTreeNode<N, B, C, F, S>,
    B extends BlueprintNode.Scoped<B, N, C, ? extends FeatureData<?, ?, F, N>>,
    C extends SokolComponent.Scoped<C, ?, ? extends FeatureProfile<?, ?, ? extends FeatureData<?, ?, F, N>>>,
    F extends FeatureInstance<F, ? extends FeatureData<?, ?, F, N>>,
    S extends ItemStack.Scoped<S, B>
> extends MutableAbstractMapNode<N> implements TreeNode.Scoped<N, B, C, F, S> {
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
    @Override public Optional<? extends FeatureData<?, ?, F, N>> featureData(String key) { return feature(key).map(i -> i.asData()); }

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
}
