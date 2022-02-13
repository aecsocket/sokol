package com.github.aecsocket.sokol.core.event;

import com.github.aecsocket.minecommons.core.InputType;
import com.github.aecsocket.minecommons.core.event.Cancellable;
import com.github.aecsocket.minecommons.core.vector.cartesian.Vector3;
import com.github.aecsocket.sokol.core.TreeNode;

import java.util.Optional;

public final class ItemEvent {
    private ItemEvent() {}

    public interface Hold<N extends TreeNode.Scoped<N, ?, ?, ?, ?>> extends NodeEvent<N> {}

    public interface Input<N extends TreeNode.Scoped<N, ?, ?, ?, ?>> extends NodeEvent<N>, Cancellable {
        InputType input();
    }

    public interface GameClick<N extends TreeNode.Scoped<N, ?, ?, ?, ?>> extends NodeEvent<N>, Cancellable {
        Optional<Vector3> clickedPos();
    }

    public interface SwapFrom<N extends TreeNode.Scoped<N, ?, ?, ?, ?>> {}
}
