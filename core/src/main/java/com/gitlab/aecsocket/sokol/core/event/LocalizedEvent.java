package com.gitlab.aecsocket.sokol.core.event;

import com.gitlab.aecsocket.sokol.core.Node;

import java.util.Locale;

public interface LocalizedEvent<N extends Node.Scoped<N, ?, ?, ?>>
        extends NodeEvent<N> {
    Locale locale();
}
