package com.gitlab.aecsocket.sokol.core.impl;

import com.gitlab.aecsocket.minecommons.core.event.EventDispatcher;
import com.gitlab.aecsocket.minecommons.core.node.NodePath;
import com.gitlab.aecsocket.sokol.core.Node;
import com.gitlab.aecsocket.sokol.core.Tree;
import com.gitlab.aecsocket.sokol.core.event.NodeEvent;
import com.gitlab.aecsocket.sokol.core.stat.StatMap;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemUser;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public record BasicTree<N extends Node.Scoped<N, ?, ?, ?, ?>>(
        N root,
        EventDispatcher<NodeEvent<N>> events,
        StatMap stats,
        List<NodePath> incomplete,
        Locale locale,
        @Nullable ItemUser user
) implements Tree<N> {
    public BasicTree(N root, ItemUser user) {
        this(root, new EventDispatcher<>(), new StatMap(), new ArrayList<>(), user.locale(), user);
    }

    public BasicTree(N root, Locale locale) {
        this(root, new EventDispatcher<>(), new StatMap(), new ArrayList<>(), locale, null);
    }

    @Override public void addIncomplete(NodePath path) { incomplete.add(path); }

    @Override
    public <E extends NodeEvent<N>> E call(E event) {
        return events.call(event);
    }
}
