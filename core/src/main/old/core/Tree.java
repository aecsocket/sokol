package com.gitlab.aecsocket.sokol.core;

import com.gitlab.aecsocket.minecommons.core.event.EventDispatcher;
import com.gitlab.aecsocket.minecommons.core.node.NodePath;
import com.gitlab.aecsocket.sokol.core.event.NodeEvent;
import com.gitlab.aecsocket.sokol.core.stat.StatMap;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemUser;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Locale;

public interface Tree<N extends Node.Scoped<N, ?, ?, ?, ?>> {
    N root();
    EventDispatcher<NodeEvent<N>> events();
    StatMap stats();
    Locale locale();
    @Nullable ItemUser user();

    List<NodePath> incomplete();
    void addIncomplete(NodePath path);
    default boolean complete() { return incomplete().isEmpty(); }

    <E extends NodeEvent<N>> E call(E event);
}
