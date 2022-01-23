package com.github.aecsocket.sokol.core.api;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public interface FeatureData<
        F extends Feature<?>,
        N extends Node.Scoped<N, ?, ?, ?, ?>,
        B extends Blueprint.Scoped<B, ?, ?, ?>,
        I extends FeatureInstance<F, N, ?>
> {
    F type();
    B parent();

    void save(ConfigurationNode node) throws SerializationException;
    I load(N node);
}
