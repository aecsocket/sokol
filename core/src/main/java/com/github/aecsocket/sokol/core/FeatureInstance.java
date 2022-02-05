package com.github.aecsocket.sokol.core;

import com.github.aecsocket.sokol.core.stat.StatIntermediate;

public interface FeatureInstance<
    I extends FeatureInstance<I, D, N>,
    D extends FeatureData<D, ?, I, N>,
    N extends TreeNode.Scoped<N, ?, ?, ?, ?>
> {
    D asData();

    I copy();

    void build(Tree<N> tree, N node, StatIntermediate stats);
}
