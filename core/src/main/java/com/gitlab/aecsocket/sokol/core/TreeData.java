package com.gitlab.aecsocket.sokol.core;

import com.gitlab.aecsocket.minecommons.core.event.EventDispatcher;
import com.gitlab.aecsocket.sokol.core.event.NodeEvent;
import com.gitlab.aecsocket.sokol.core.stat.StatMap;

public interface TreeData<N extends Node> {
    EventDispatcher<? extends NodeEvent<? extends N>> events();
    StatMap stats();

    interface Scoped<N extends Node.Scoped<N, ?, ?>> extends TreeData<N> {
        @Override EventDispatcher<NodeEvent<N>> events();
    }
}
