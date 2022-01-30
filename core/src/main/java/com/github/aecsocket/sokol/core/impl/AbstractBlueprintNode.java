package com.github.aecsocket.sokol.core.impl;

import java.util.HashMap;
import java.util.Map;

import com.github.aecsocket.minecommons.core.node.MutableAbstractMapNode;
import com.github.aecsocket.sokol.core.BlueprintNode;
import com.github.aecsocket.sokol.core.FeatureData;
import com.github.aecsocket.sokol.core.FeatureInstance;
import com.github.aecsocket.sokol.core.FeatureProfile;
import com.github.aecsocket.sokol.core.SokolComponent;
import com.github.aecsocket.sokol.core.TreeNode;
import com.github.aecsocket.sokol.core.world.ItemStack;

public abstract class AbstractBlueprintNode<
    B extends AbstractBlueprintNode<B, N, C, F>,
    N extends TreeNode.Scoped<N, B, C, ? extends FeatureInstance<?, F>, ? extends ItemStack.Scoped<?, B>>,
    C extends SokolComponent.Scoped<C, ?, ? extends FeatureProfile<?, ?, F>>,
    F extends FeatureData<F, ?, ? extends FeatureInstance<?, F>>
> extends MutableAbstractMapNode<B> implements BlueprintNode.Scoped<B, N, C, F> {
    protected final C value;
    protected final Map<String, F> features;

    public AbstractBlueprintNode(AbstractBlueprintNode<B, N, C, F> o) {
        super(o);
        value = o.value;
        features = new HashMap<>(o.features);
    }

    
}
