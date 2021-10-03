package com.gitlab.aecsocket.sokol.core;

import com.gitlab.aecsocket.sokol.core.registry.Keyed;

public interface FeatureType<
        F extends Feature.Scoped<F, N>,
        N extends Node
> extends Keyed {
    F create(N node);
}
