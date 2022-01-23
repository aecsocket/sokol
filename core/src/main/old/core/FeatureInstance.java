package com.gitlab.aecsocket.sokol.core;

import com.gitlab.aecsocket.sokol.core.stat.StatIntermediate;

public interface FeatureInstance<D, N extends Node.Scoped<N, ?, ?, ?, ?>> {
    Feature<?, ?> type();

    void build(Tree<N> treeCtx, StatIntermediate stats);

    FeatureInstance<D, N> copy(N parent);

    D save();
}
