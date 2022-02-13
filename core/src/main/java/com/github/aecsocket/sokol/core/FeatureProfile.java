package com.github.aecsocket.sokol.core;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public interface FeatureProfile<
    F extends Feature<?>,
    D extends FeatureData<?, ?, ?>
> {
    F type();

    D setUp();
    D load(ConfigurationNode node) throws SerializationException;
}
