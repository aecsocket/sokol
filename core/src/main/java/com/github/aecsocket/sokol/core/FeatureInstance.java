package com.github.aecsocket.sokol.core;

import com.github.aecsocket.sokol.core.stat.StatIntermediate;

public interface FeatureInstance<
    D extends FeatureData<?, ?, N>,
    N extends TreeNode.Scoped<N, ?, ?, ?, ?>
> {
    D asData();

    void build(Tree<N> tree, N parent, StatIntermediate stats);
}
