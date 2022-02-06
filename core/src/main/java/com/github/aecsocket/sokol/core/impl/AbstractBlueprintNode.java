package com.github.aecsocket.sokol.core.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import com.github.aecsocket.minecommons.core.node.MutableAbstractMapNode;
import com.github.aecsocket.sokol.core.*;
import com.github.aecsocket.sokol.core.world.ItemStack;

import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class AbstractBlueprintNode<
    B extends AbstractBlueprintNode<B, N, C, F>,
    N extends TreeNode.Scoped<N, B, C, ? extends FeatureInstance<?, F, N>, ? extends ItemStack.Scoped<?, B>>,
    C extends SokolComponent.Scoped<C, ?, ? extends FeatureProfile<?, ?, F>>,
    F extends FeatureData<F, ?, ? extends FeatureInstance<?, F, N>, N>
> extends MutableAbstractMapNode<B> implements BlueprintNode.Scoped<B, N, C, F> {
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

    @Override public C value() { return value; }
    
    @Override public Map<String, F> featureData() { return new HashMap<>(featureData); }
    @Override public boolean hasFeature(String key) { return featureData.containsKey(key); }
    @Override public Optional<F> featureData(String key) { return Optional.ofNullable(featureData.get(key)); }

    @Override
    public B set(String key, B val) {
        value.slot(key)
            .orElseThrow(() -> new IllegalArgumentException("No slot with ID `" + key + "` on component `" + value.id() + "`"))
            .compatible(val, self());
        return super.set(key, val);
    }

    @Override
    public void visitBlueprints(Consumer<BlueprintNode> visitor) {
        visit(visitor::accept);
    }

    @Override
    public boolean complete() {
        return SokolNode.complete(this);
    }
}
