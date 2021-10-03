package com.gitlab.aecsocket.sokol.core;

public interface Feature<N extends Node> {
    FeatureType<?, N> type();
    N parent();

    Feature<N> copy();

    interface Scoped<F extends Scoped<F, N>, N extends Node> extends Feature<N> {
        @Override FeatureType<F, N> type();
        @Override N parent();

        @Override F copy();
    }
}
