package com.github.aecsocket.sokol.core.impl;

import com.github.aecsocket.sokol.core.SokolPlatform;
import com.github.aecsocket.sokol.core.api.*;
import com.github.aecsocket.sokol.core.context.Context;
import com.github.aecsocket.sokol.core.event.NodeEvent;
import com.github.aecsocket.sokol.core.world.ItemStack;
import com.gitlab.aecsocket.minecommons.core.i18n.I18N;
import com.gitlab.aecsocket.minecommons.core.node.MutableAbstractMapNode;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;
import java.util.function.Consumer;

public abstract class AbstractNode<
        N extends AbstractNode<N, B, C, I, S>,
        B extends Blueprint.Scoped<B, N, C, ?>,
        C extends Component.Scoped<C, ?, ? extends FeatureConfig<?, N, C, ? extends FeatureData<?, N, ?, I>>>,
        I extends FeatureInstance.Scoped<I, ?, N, ?>,
        S extends ItemStack.Scoped<S, B>
> extends MutableAbstractMapNode<N> implements Node.Mutable<N, B, C, I, S> {
    protected final C value;
    protected final Context context;
    protected final Map<String, I> features;
    protected Tree<N> tree;

    private <D extends FeatureData<?, N, ?, I>> Map<String, I> buildFeatureInstances(Map<String, D> data) {
        Map<String, I> result = new HashMap<>();
        for (var entry : value.features().entrySet()) {
            String key = entry.getKey();
            D value = data.get(key);
            result.put(key, (value == null
                    ? entry.getValue().setup()
                    : value).load(self()));
        }
        for (var entry : data.entrySet()) {
            result.put(entry.getKey(), entry.getValue().load(self()));
        }
        return result;
    }

    public AbstractNode(AbstractNode<N, B, C, I, S> o) {
        super(o);
        value = o.value;
        context = o.context;
        tree = o.tree;

        features = new HashMap<>();
        for (var entry : o.features.entrySet()) {
            features.put(entry.getKey(), entry.getValue().copy());
        }
    }

    protected <D extends FeatureData<?, N, ?, I>> AbstractNode(C value, Context context, Map<String, D> featureData, @Nullable Key<N> key, @Nullable Tree<N> tree) {
        super(key);
        this.value = value;
        this.context = context;
        this.tree = tree;
        this.features = buildFeatureInstances(featureData);
    }

    public <D extends FeatureData<?, N, ?, I>> AbstractNode(C value, Context context, Map<String, D> featureData, N parent, String key, Tree<N> tree) {
        super(parent, key);
        this.value = value;
        this.context = context;
        this.tree = tree;
        this.features = buildFeatureInstances(featureData);
    }
    public <D extends FeatureData<?, N, ?, I>> AbstractNode(C value, Context context, Map<String, D> featureData, N parent, String key) {
        super(parent, key);
        this.value = value;
        this.context = context;
        build();
        this.features = buildFeatureInstances(featureData);
    }

    public <D extends FeatureData<?, N, ?, I>> AbstractNode(C value, Context context, Map<String, D> featureData, Tree<N> tree) {
        this.value = value;
        this.context = context;
        this.tree = tree;
        this.features = buildFeatureInstances(featureData);
    }
    public <D extends FeatureData<?, N, ?, I>> AbstractNode(C value, Context context, Map<String, D> featureData) {
        this.value = value;
        this.context = context;
        build();
        this.features = buildFeatureInstances(featureData);
    }

    protected abstract SokolPlatform platform();

    @Override public C value() { return value; }
    @Override public Context context() { return context; }

    @Override public Map<String, I> features() { return features; }
    @Override public Optional<I> feature(String key) { return Optional.ofNullable(features.get(key)); }
    @Override public Set<String> featureKeys() { return features.keySet(); }
    @Override public boolean hasFeature(String key) { return features.containsKey(key); }

    @Override public Tree<N> tree() { return tree; }
    @Override public void tree(Tree<N> tree) { this.tree = tree; }

    @Override
    public void build() {
        tree = Tree.build(self());
    }

    @Override
    public N set(String key, N val) {
        value.slot(key)
                .orElseThrow(() -> new IllegalArgumentException("No slot with ID `" + key + "` on component `" + value.id() + "`"))
                .compatible(val, self());
        return super.set(key, val);
    }

    @Override
    public void visitNodes(Consumer<Node> visitor) {
        visit(visitor::accept);
    }

    public void setUnsafe(String key, @Nullable N value) {
        if (value == null)
            children.remove(key);
        else
            children.put(key, value);
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
