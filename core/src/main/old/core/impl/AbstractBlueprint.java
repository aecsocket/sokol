package com.gitlab.aecsocket.sokol.core.impl;

import com.gitlab.aecsocket.minecommons.core.node.MutableAbstractMapNode;
import com.gitlab.aecsocket.sokol.core.*;
import com.gitlab.aecsocket.sokol.core.context.Context;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public abstract class AbstractBlueprint<
        B extends AbstractBlueprint<B, F, C, N>,
        F extends FeatureInstance<?, N>,
        C extends Component.Scoped<C, ?, ? extends Feature<? extends F, N>, N>,
        N extends Node.Mutable<N, ?, C, F, B>
> extends MutableAbstractMapNode<B> implements Blueprint.Mutable<B, C, N> {
    protected final C value;
    protected final Map<String, Object> featureData = new HashMap<>();

    public AbstractBlueprint(AbstractBlueprint<B, F, C, N> o) {
        super(o);
        value = o.value;
        // we assume that the feature data is immutable
        featureData.putAll(o.featureData);
    }

    public AbstractBlueprint(@Nullable Key<B> key, C value) {
        super(key);
        this.value = value;
    }

    public AbstractBlueprint(B parent, String key, C value) {
        super(parent, key);
        this.value = value;
    }

    public AbstractBlueprint(C value) {
        this.value = value;
    }

    @Override public C value() { return value; }
    @Override public Map<String, ?> featureData() { return new HashMap<>(featureData); }

    @Override
    public void visitBlueprints(Consumer<Blueprint> visitor) {
        visit(visitor::accept);
    }

    protected abstract N createNode(Context context);

    @SuppressWarnings("unchecked")
    private <D, E extends Feature<? extends F, D>> F feature(E feature, Object data) {
        return feature.load((D) data);
    }

    @Override
    public N build(Context context) {
        Map<String, F> features = new HashMap<>();
        for (var entry : value.features().entrySet()) {
            F feature = feature(entry.getValue(), featureData.get(entry.getKey()));
        }
        N root = createNode(context);
        for (var entry : children.entrySet()) {
            root.set(entry.getKey(), entry.getValue().build(context));
        }
        return root;
    }
}
