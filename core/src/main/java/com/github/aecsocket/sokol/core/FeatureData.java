package com.github.aecsocket.sokol.core;

public interface FeatureData<
    D extends FeatureData<D, P, I, N>,
    P extends FeatureProfile<P, ?, D>,
    I extends FeatureInstance<I, D, N>,
    N extends TreeNode.Scoped<N, ?, ?, ?, ?>
> {
    P profile();

    I asInstance(N node);
}
