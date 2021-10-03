package com.gitlab.aecsocket.sokol.core.TEST;

import com.gitlab.aecsocket.sokol.core.component.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;

public interface Node {
    @Nullable Node parent();
    @Nullable String key();
    NodePath path();

    Node root();
    Optional<? extends Node> node(String... path);

    interface Scoped<N extends Scoped<N>> extends Node {
        @Nullable N parent();

        N root();
        Optional<N> node(String... path);
        void set(String key, N val);
    }

    class Impl implements Scoped<Impl> {
        private Impl parent;
        private String key;
        private final Map<String, Impl> children = new HashMap<>();

        public Impl(Impl parent, String key) {
            this.parent = parent;
            this.key = key;
        }

        @Override public Node.@Nullable Impl parent() { return parent; }
        @Override public @Nullable String key() { return key; }

        public void parent(@Nullable Impl parent, @Nullable String key) {
            this.parent = parent;
            this.key = key;
        }

        @Override
        public NodePath path() {
            LinkedList<String> path = new LinkedList<>();
            Node current = this;
            do {
                path.addFirst(current.key());
            } while ((current = current.parent()) != null);
            return NodePath.path(path);
        }

        @Override
        public Optional<Impl> node(String... path) {
            if (path.length == 0)
                return Optional.empty();
            Impl next = children.get(path[0]);
            if (next == null)
                return Optional.empty();
            String[] newPath = Arrays.copyOfRange(path, 1, path.length);
            return next.node(newPath);
        }

        @Override
        public void set(String key, Impl val) {
            Impl old = children.put(key, val);
            if (old != null)
                old.parent(null, null);
            val.parent(this, key);
        }

        @Override
        public Impl root() { return parent == null ? this : parent.root(); }
    }
}
