package com.github.aecsocket.sokol.core;

import com.github.aecsocket.sokol.core.item.ItemStack;
import com.github.aecsocket.sokol.core.item.ItemState;
import com.github.aecsocket.sokol.core.stat.StatIntermediate;

public interface FeatureInstance<
    P extends FeatureProfile<?, D>,
    D extends FeatureData<P, ?, N>,
    N extends TreeNode.Scoped<N, B, ?, ?, S, T>,
    B extends BlueprintNode.Scoped<B, N, ?, ?>,
    S extends ItemStack.Scoped<T, S, B>,
    T extends ItemState.Scoped<T>
> {
    P profile();

    N parent();
    void build(Tree<N, B, S, T> tree, N parent, StatIntermediate stats);

    D asData();
}
