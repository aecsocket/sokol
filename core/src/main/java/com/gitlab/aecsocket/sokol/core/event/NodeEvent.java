package com.gitlab.aecsocket.sokol.core.event;

import com.gitlab.aecsocket.sokol.core.Node;

import java.util.Locale;

public interface NodeEvent<N extends Node.Scoped<N, ?, ?, ?>> {
    N node();
    Locale locale();
}
