package com.gitlab.aecsocket.sokol.core.tree;

import com.gitlab.aecsocket.sokol.core.component.Component;
import com.gitlab.aecsocket.sokol.core.component.System;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

public class BasicTreeNode<N extends BasicTreeNode<N, C, B, Y>, C extends Component.Scoped<C, ?, B>, B extends System.Base<Y>, Y extends System<B>>
        implements ScopedTreeNode<N, C, B, Y> {
    protected final C value;
    protected final Map<String, N> children;
    protected final Map<String, Y> systems;
    protected String key;
    protected N parent;

    public BasicTreeNode(C value, Map<String, N> children, Map<String, Y> systems, String key, N parent) {
        this.value = value;
        this.children = children;
        this.systems = systems;
        this.key = key;
        this.parent = parent;
    }

    public BasicTreeNode(C value, Map<String, N> children, Map<String, Y> systems) {
        this.value = value;
        this.children = children;
        this.systems = systems;
    }

    public BasicTreeNode(C value) {
        this(value, new HashMap<>(), new HashMap<>());
    }

    @Override public @NotNull C value() { return value; }

    @Override public @NotNull Map<String, N> children() { return children; }
    @Override public @Nullable N child(String key) { return children.get(key); }
    @Override public void child(String key, N child) {
        if (!value.slots().containsKey(key))
            throw new IllegalArgumentException("Cannot add child [" + key + "] to tree node without corresponding slot in component");
        children.put(key, child);
    }

    @Override public @NotNull Map<String, Y> systems() { return systems; }
    @Override public @Nullable Y system(String id) { return systems.get(id); }
    @Override
    public void system(Y system) {
        String id = system.base().id();
        if (!value.baseSystems().containsKey(id))
            throw new IllegalArgumentException("Cannot add system [" + id + "] to tree node without corresponding base system in component");
        systems.put(id, system);
    }

    @Override public @Nullable String key() { return key; }
    @Override public @Nullable N parent() { return parent; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BasicTreeNode<?, ?, ?, ?> that = (BasicTreeNode<?, ?, ?, ?>) o;
        return value.equals(that.value) && children.equals(that.children) && systems.equals(that.systems) && Objects.equals(key, that.key) && Objects.equals(parent, that.parent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, children, systems, key, parent);
    }

    @Override
    public String toString() {
        StringJoiner details = new StringJoiner(", ");
        if (children.size() > 0)
            details.add("children=" + children);
        if (systems.size() > 0)
            details.add("systems=" + systems);
        return value.id() + '{' + details + '}';
    }
}
