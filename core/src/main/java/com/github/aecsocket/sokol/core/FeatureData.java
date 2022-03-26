package com.github.aecsocket.sokol.core;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public interface FeatureData<
    P extends FeatureProfile<?, ?>,
    I extends FeatureInstance<?, ?, N>,
    N extends TreeNode.Scoped<N, ?, ?, ?, ?>
> {
    P profile();

    void save(ConfigurationNode node) throws SerializationException;

    I asInstance(N node);
}
