package com.gitlab.aecsocket.sokol.core;

import com.gitlab.aecsocket.minecommons.core.event.EventDispatcher;
import com.gitlab.aecsocket.sokol.core.event.NodeEvent;
import com.gitlab.aecsocket.sokol.core.node.NodePath;
import com.gitlab.aecsocket.sokol.core.stat.StatMap;

import java.util.List;

public interface TreeData<N extends Node> {
    EventDispatcher<? extends NodeEvent<? extends N>> events();
    StatMap stats();

    List<NodePath> incomplete();
    void addIncomplete(NodePath path);
    default boolean complete() { return incomplete().isEmpty(); }

    interface Scoped<N extends Node.Scoped<N, ?, ?>> extends TreeData<N> {
        @Override EventDispatcher<NodeEvent<N>> events();
    }
}
