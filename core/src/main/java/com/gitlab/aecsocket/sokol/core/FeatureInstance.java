package com.gitlab.aecsocket.sokol.core;

import com.gitlab.aecsocket.sokol.core.event.NodeEvent;
import com.gitlab.aecsocket.sokol.core.stat.StatIntermediate;

public interface FeatureInstance<N extends Node.Scoped<N, ?, ?, ?>> {
    Feature<?, N> type();
    N parent();

    void build(NodeEvent<N> event, StatIntermediate stats);

    FeatureInstance<N> copy(N parent);
}
