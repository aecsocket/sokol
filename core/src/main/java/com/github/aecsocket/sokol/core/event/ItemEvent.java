package com.github.aecsocket.sokol.core.event;

import com.github.aecsocket.sokol.core.TreeNode;

public final class ItemEvent {
    private ItemEvent() {}

    public interface Hold<N extends TreeNode.Scoped<N, ?, ?, ?, ?>> extends NodeEvent<N> {}
}
