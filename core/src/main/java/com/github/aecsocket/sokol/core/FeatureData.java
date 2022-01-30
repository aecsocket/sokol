package com.github.aecsocket.sokol.core;

public interface FeatureData<
    D extends FeatureData<D, P, I>,
    P extends FeatureProfile<P, ?, D>,
    I extends FeatureInstance<I, D>
> {
    P profile();

    I load(); // add ctx here
}
