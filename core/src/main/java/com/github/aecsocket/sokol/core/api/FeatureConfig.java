package com.github.aecsocket.sokol.core.api;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public interface FeatureConfig<
        F extends Feature<?>,
        N extends Node.Scoped<N, ?, ?, ?, ?>,
        D extends FeatureData<F, N, ?, ?>
> {
    F type();

    D setup();
    D load(ConfigurationNode node) throws SerializationException;
}
