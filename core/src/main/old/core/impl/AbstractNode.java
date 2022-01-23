package com.gitlab.aecsocket.sokol.core.impl;

import com.gitlab.aecsocket.minecommons.core.node.AbstractMapNode;
import com.gitlab.aecsocket.sokol.core.*;
import com.gitlab.aecsocket.sokol.core.context.Context;
import com.gitlab.aecsocket.sokol.core.wrapper.Item;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;

public abstract class AbstractNode<
        N extends AbstractNode<N, I, C, F, B>,
        I extends Item.Scoped<I, N>,
        C extends Component.Scoped<C, ?, ? extends Feature<? extends F, N>, N>,
        F extends FeatureInstance<?, N>,
        B extends Blueprint.Mutable<B, C, N>
> extends AbstractMapNode<N> implements Node.Scoped<N, I, C, F, B> {
    public static final String TAG_REQUIRED = "required";

    public static boolean required(Slot slot) {
        return slot.tagged(TAG_REQUIRED);
    }

    protected record NodeKey<N extends Node>(N parent, String key) {}

    protected final C value;
    protected final Context context;
    protected final Map<String, F> features;
    protected TreeData<N> tree;

    protected abstract F copyFeature(F val);

    // 334950
    public AbstractNode(AbstractNode<N, I, C, F, B> o) {
        super(o.key);
        value = o.value;
        context = o.context;
        tree = o.tree;
        for (var entry : o.children.entrySet()) {
            children.put(entry.getKey(), entry.getValue().copy());
        }
        // TODO ???
        Map<String, F> features = new HashMap<>();
        for (var entry : o.features.entrySet()) {
            features.put(entry.getKey(), copyFeature(entry.getValue()));
        }
        this.features = Collections.unmodifiableMap(features);
    }

    protected AbstractNode(C value, Context context, Map<String, F> features, @Nullable TreeData<N> tree, @Nullable Key<N> key) {
        super(key);
        this.value = value;
        this.context = context;
        this.features = Collections.unmodifiableMap(features);
        this.tree = tree;
    }

    public AbstractNode(C value, Context context, TreeData<N> tree, N parent, String key) {
        super(parent, key);
        this.value = value;
        this.context = context;
        features = Collections.emptyMap();
        this.tree = tree;
    }

    public AbstractNode(C value, Context context, TreeData<N> tree) {
        super();
        this.value = value;
        this.context = context;
        features = Collections.emptyMap();
        this.tree = tree;
    }

    @Override public C value() { return value; }
    @Override public TreeData<N> tree() { return tree; }

    @Override public Map<String, F> features() { return new HashMap<>(features); }
    @Override public Set<String> featureKeys() { return features.keySet(); }
    @Override public Collection<F> featureValues() { return features.values(); }
    @Override public Optional<F> feature(String key) { return Optional.ofNullable(features.get(key)); }

    protected abstract B createBlueprint(Map<String, Object> featureData);

    @Override
    public B asBlueprint() {
        Map<String, Object> featureData = new HashMap<>();
        for (var entry : features.entrySet()) {
            featureData.put(entry.getKey(), entry.getValue().save());
        }
        B blueprint = createBlueprint(featureData);
        for (var entry : children.entrySet()) {
            blueprint.set(entry.getKey(), entry.getValue().asBlueprint());
        }
        return blueprint;
    }

    @Override
    public String toString() {
        return value.id() + children;
    }
}
