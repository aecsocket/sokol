package com.gitlab.aecsocket.sokol.core.impl;

import com.gitlab.aecsocket.minecommons.core.event.EventDispatcher;
import com.gitlab.aecsocket.sokol.core.Node;
import com.gitlab.aecsocket.sokol.core.TreeData;
import com.gitlab.aecsocket.sokol.core.event.NodeEvent;
import com.gitlab.aecsocket.sokol.core.stat.StatMap;

public record BasicTreeData<N extends Node.Scoped<N, ?, ?>>(
        EventDispatcher<NodeEvent<N>> events,
        StatMap stats
) implements TreeData.Scoped<N> {
    public static <N extends Node.Scoped<N, ?, ?>> BasicTreeData<N> blank() {
        return new BasicTreeData<>(new EventDispatcher<>(), new StatMap());
    }
}
