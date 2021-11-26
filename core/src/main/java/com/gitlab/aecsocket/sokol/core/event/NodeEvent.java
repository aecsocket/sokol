package com.gitlab.aecsocket.sokol.core.event;

import com.gitlab.aecsocket.sokol.core.Node;

public interface NodeEvent<N extends Node.Scoped<N, ?, ?, ?>> {
    N node();
}
