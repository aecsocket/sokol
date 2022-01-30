package com.github.aecsocket.sokol.core;

import java.util.List;
import java.util.function.Function;

import com.github.aecsocket.minecommons.core.event.EventDispatcher;
import com.github.aecsocket.minecommons.core.node.NodePath;
import com.github.aecsocket.sokol.core.event.NodeEvent;
import com.github.aecsocket.sokol.core.stat.StatMap;

public record Tree<N extends TreeNode.Scoped<N, ?, ?, ?, ?>>(
    N root,
    EventDispatcher<NodeEvent<N>> events,
    StatMap stats,
    List<NodePath> incomplete
) {
    public static final String REQUIRED = "required";

    public static boolean required(NodeSlot slot) {
        return slot.tagged(REQUIRED);
    }

    public void incomplete(NodePath path) {
        incomplete.add(path);
    }

    public boolean complete() {
        return incomplete.isEmpty();
    }

    public <E extends NodeEvent<N>> E call(E event) {
        return events.call(event);
    }

    public <E extends NodeEvent<N>> E andCall(Function<N, E> event) {
        return events.call(event.apply(root));
    }
}
