package com.gitlab.aecsocket.sokol.core.event;

import com.gitlab.aecsocket.sokol.core.Feature;
import com.gitlab.aecsocket.sokol.core.Node;

public interface FeatureEvent<N extends Node.Scoped<N, ?, ?>, F extends Feature<N>> extends NodeEvent<N> {
    F feature();
}
