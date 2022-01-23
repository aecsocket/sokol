package com.github.aecsocket.sokol.core.event;

import com.github.aecsocket.sokol.core.api.Blueprint;
import com.github.aecsocket.sokol.core.api.Node;
import com.github.aecsocket.sokol.core.world.ItemStack;
import com.gitlab.aecsocket.minecommons.core.event.Cancellable;

public interface NodeEvent<N extends Node.Scoped<N, ?, ?, ?, ?>> {
    N node();

    default boolean call() {
        return node().tree().call(this) instanceof Cancellable cancel && cancel.cancelled();
    }

    interface CreateItem<
            N extends Node.Scoped<N, B, ?, ?, S>,
            B extends Blueprint.Scoped<B, N, ?, ?>,
            S extends ItemStack.Scoped<S, B>
    > extends NodeEvent<N> {
        S item();
    }
}
