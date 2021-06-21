package com.gitlab.aecsocket.sokol.core.system;

import com.gitlab.aecsocket.sokol.core.tree.TreeEvent;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractSystem<N extends TreeNode.Scoped<N, ?, ?, ?, ?>> implements System<N> {
    public static abstract class Instance<N extends TreeNode.Scoped<N, ?, ?, ?, ?>> implements System.Instance<N> {
        protected final N parent;

        public Instance(N parent) {
            this.parent = parent;
        }

        @Override public @NotNull N parent() { return parent; }

        protected <T> T stat(String key) {
            return parent().stats().value(key);
        }

        protected <E extends TreeEvent> E call(E event) {
            return parent().events().call(event);
        }

        @Override public String toString() {
            return "<%s>".formatted(base().id());
        }
    }

    @Override public String toString() { return "<%s>".formatted(id()); }
}
