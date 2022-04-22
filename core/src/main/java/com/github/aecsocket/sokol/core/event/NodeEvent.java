package com.github.aecsocket.sokol.core.event;

import com.github.aecsocket.minecommons.core.event.Cancellable;
import com.github.aecsocket.sokol.core.TreeNode;

public interface NodeEvent<N extends TreeNode.Scoped<N, ?, ?, ?, ?>> {
    N node();

    default boolean call() {
        return node().tree().call(this) instanceof Cancellable cancel && cancel.cancelled();
    }
}
