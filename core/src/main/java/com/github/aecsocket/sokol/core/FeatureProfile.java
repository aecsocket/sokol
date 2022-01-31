package com.github.aecsocket.sokol.core;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public interface FeatureProfile<
    P extends FeatureProfile<P, F, D>,
    F extends Feature<F, P>,
    D extends FeatureData<D, P, ?, ?>
> {
    F type();

    D setUp();
    D load(ConfigurationNode node) throws SerializationException;
}
