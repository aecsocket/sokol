package com.gitlab.aecsocket.sokol.core.impl;

import com.gitlab.aecsocket.minecommons.core.event.EventDispatcher;
import com.gitlab.aecsocket.sokol.core.Node;
import com.gitlab.aecsocket.sokol.core.TreeContext;
import com.gitlab.aecsocket.sokol.core.event.NodeEvent;
import com.gitlab.aecsocket.sokol.core.node.NodePath;
import com.gitlab.aecsocket.sokol.core.stat.StatMap;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemUser;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public record BasicTreeContext<N extends Node.Scoped<N, ?, ?, ?>>(
        EventDispatcher<NodeEvent<N>> events,
        StatMap stats,
        List<NodePath> incomplete,
        Locale locale,
        @Nullable ItemUser user
) implements TreeContext<N> {
    public BasicTreeContext(ItemUser user) {
        this(new EventDispatcher<>(), new StatMap(), new ArrayList<>(), user.locale(), user);
    }

    public BasicTreeContext(Locale locale) {
        this(new EventDispatcher<>(), new StatMap(), new ArrayList<>(), locale, null);
    }

    @Override public void addIncomplete(NodePath path) { incomplete.add(path); }

    @Override
    public <E extends NodeEvent<N>> E call(E event) {
        return events.call(event);
    }
}
