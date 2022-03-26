package com.github.aecsocket.sokol.core;

import com.github.aecsocket.sokol.core.stat.StatIntermediate;

public interface FeatureInstance<
    P extends FeatureProfile<?, D>,
    D extends FeatureData<P, ?, N>,
    N extends TreeNode.Scoped<N, ?, ?, ?, ?>
> {
    P profile();

    N parent();
    void build(Tree<N> tree, N parent, StatIntermediate stats);

    D asData();
}
