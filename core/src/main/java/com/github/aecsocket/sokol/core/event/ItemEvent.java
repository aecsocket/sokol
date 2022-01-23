package com.github.aecsocket.sokol.core.event;

import com.github.aecsocket.sokol.core.api.Node;

public final class ItemEvent {
    private ItemEvent() {}

    public interface Hold<N extends Node.Scoped<N, ?, ?, ?, ?>> extends NodeEvent<N> {}
}
