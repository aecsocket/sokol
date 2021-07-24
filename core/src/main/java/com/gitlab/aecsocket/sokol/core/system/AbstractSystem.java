package com.gitlab.aecsocket.sokol.core.system;

import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An abstract system with some methods implemented.
 */
public abstract class AbstractSystem implements System {
    protected static final String keyListenerPriority = "listener_priority";

    protected final int listenerPriority;

    public AbstractSystem(int listenerPriority) {
        this.listenerPriority = listenerPriority;
    }

    public int listenerPriority() { return listenerPriority; }

    /**
     * An abstract system instance.
     */
    public static abstract class Instance implements System.Instance {
        protected final TreeNode parent;

        public Instance(TreeNode parent) {
            this.parent = parent;
        }

        @Override public TreeNode parent() { return parent; }

        protected String lck(String key) {
            return "system." + base().id() + "." + key;
        }

        protected <S extends System.Instance> @Nullable S softDepend(Key<S> key) {
            return parent.system(key).orElse(null);
        }

        protected <S extends System.Instance> @Nullable S softDepend(Class<S> type) {
            return parent.system(type).orElse(null);
        }

        protected <S extends System.Instance> S depend(Key<S> key) {
            return parent.system(key)
                    .orElseThrow(() -> new IllegalStateException("System [" + base().id() + "] depends on [" + key.id() + "]"));
        }

        protected <S extends System.Instance> S depend(Class<S> type) {
            return parent.system(type)
                    .orElseThrow(() -> new IllegalStateException("System [" + base().id() + "] depends on [" + type.getSimpleName() + "]"));
        }

        @Override public String toString() {
            return "<%s>".formatted(base().id());
        }
    }

    @Override public String toString() { return "(%s)".formatted(id()); }
}
