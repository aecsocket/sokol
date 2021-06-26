package com.gitlab.aecsocket.sokol.core.system;

import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import org.jetbrains.annotations.NotNull;

/**
 * An abstract system with some methods implemented.
 */
public abstract class AbstractSystem implements System {
    /**
     * An abstract system instance.
     */
    public static abstract class Instance implements System.Instance {
        protected final TreeNode parent;

        public Instance(TreeNode parent) {
            this.parent = parent;
        }

        @Override public @NotNull TreeNode parent() { return parent; }

        @Override public String toString() {
            return "<%s>".formatted(base().id());
        }
    }

    @Override public String toString() { return "(%s)".formatted(id()); }
}
