package com.gitlab.aecsocket.sokol.core.event;

import com.gitlab.aecsocket.sokol.core.FeatureInstance;
import com.gitlab.aecsocket.sokol.core.Node;

public interface FeatureEvent<N extends Node.Scoped<N, ?, ?>, F extends FeatureInstance<N>> extends NodeEvent<N> {
    F feature();
}
