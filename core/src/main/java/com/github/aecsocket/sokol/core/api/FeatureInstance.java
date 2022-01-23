package com.github.aecsocket.sokol.core.api;

public interface FeatureInstance<
        F extends Feature<?>,
        N extends Node.Scoped<N, ?, ?, ?, ?>,
        D extends FeatureData<F, N, ?, ?>
> {
    F type();
    N parent();

    D save();

    interface Scoped<
            I extends Scoped<I, F, N, D>,
            F extends Feature<?>,
            N extends Node.Scoped<N, ?, ?, ?, ?>,
            D extends FeatureData<F, N, ?, ?>
    > extends FeatureInstance<F, N, D> {
        I copy();
    }
}
