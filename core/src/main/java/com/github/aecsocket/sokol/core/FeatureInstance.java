package com.github.aecsocket.sokol.core;

public interface FeatureInstance<
    I extends FeatureInstance<I, D>,
    D extends FeatureData<D, ?, I, ?>
> {
    D asData();

    I copy();
}
