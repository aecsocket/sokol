package com.gitlab.aecsocket.sokol.core;

import com.gitlab.aecsocket.sokol.core.event.NodeEvent;

public interface FeatureInstance<N extends Node.Scoped<N, ?, ?>> {
    Feature<?, N> type();
    N parent();

    void build(NodeEvent<N> event);

    FeatureInstance<N> copy();
}
