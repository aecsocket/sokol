package com.gitlab.aecsocket.sokol.core.event;

import com.gitlab.aecsocket.minecommons.core.event.Cancellable;
import com.gitlab.aecsocket.sokol.core.Node;

public interface NodeEvent<N extends Node.Scoped<N, ?, ?, ?>> {
    N node();

    default boolean call() {
        return node().tree().call(this) instanceof Cancellable cancel && cancel.cancelled();
    }
}
