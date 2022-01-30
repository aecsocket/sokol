package com.github.aecsocket.sokol.core.event;

import com.github.aecsocket.minecommons.core.event.Cancellable;
import com.github.aecsocket.sokol.core.BlueprintNode;
import com.github.aecsocket.sokol.core.TreeNode;
import com.github.aecsocket.sokol.core.world.ItemStack;

public interface NodeEvent<N extends TreeNode.Scoped<N, ?, ?, ?, ?>> {
    N node();

    default boolean call() {
        return node().tree().call(this) instanceof Cancellable cancel && cancel.cancelled();
    }

    interface CreateItem<
        N extends TreeNode.Scoped<N, B, ?, ?, S>,
        B extends BlueprintNode.Scoped<B, N, ?, ?>,
        S extends ItemStack.Scoped<S, B>
    > extends NodeEvent<N> {
        S item();
    }
}
