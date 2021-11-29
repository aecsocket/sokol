package com.gitlab.aecsocket.sokol.core.impl;

import com.gitlab.aecsocket.minecommons.core.event.EventDispatcher;
import com.gitlab.aecsocket.sokol.core.Node;
import com.gitlab.aecsocket.sokol.core.TreeContext;
import com.gitlab.aecsocket.sokol.core.event.NodeEvent;
import com.gitlab.aecsocket.sokol.core.node.NodePath;
import com.gitlab.aecsocket.sokol.core.stat.StatMap;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemUser;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Locale;

public record CompatibilityTreeContext<N extends Node.Scoped<N, ?, ?, ?>>(
        N parent,
        N child,
        TreeContext<N> parentCtx,
        TreeContext<N> childCtx
) implements TreeContext<N> {
    @Override public EventDispatcher<NodeEvent<N>> events() { return childCtx.events(); }
    @Override public StatMap stats() { return childCtx.stats(); }
    @Override public Locale locale() { return childCtx.locale(); }
    @Override public @Nullable ItemUser user() { return childCtx.user(); }
    @Override public List<NodePath> incomplete() { return childCtx.incomplete(); }
    @Override public void addIncomplete(NodePath path) { childCtx.addIncomplete(path); }
    @Override public <E extends NodeEvent<N>> E call(E event) { return childCtx.call(event); }
}
