package com.github.aecsocket.sokol.core;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public interface FeatureData<
    D extends FeatureData<D, P, I, N>,
    P extends FeatureProfile<P, ?, D>,
    I extends FeatureInstance<I, D, N>,
    N extends TreeNode.Scoped<N, ?, ?, ?, ?>
> {
    P profile();

    void save(ConfigurationNode node) throws SerializationException;

    I asInstance(N node);
}
