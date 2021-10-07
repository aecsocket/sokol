package com.gitlab.aecsocket.sokol.core;

import com.gitlab.aecsocket.sokol.core.registry.Keyed;

public interface Feature<
        F extends FeatureInstance<N>,
        N extends Node.Scoped<N, ?, ?>
> extends Keyed {
    F create(N node);
}
