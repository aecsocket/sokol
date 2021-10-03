package com.gitlab.aecsocket.sokol.core.feature;

import com.gitlab.aecsocket.sokol.core.util.TreeNode;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An abstract feature with some methods implemented.
 */
public abstract class AbstractFeature implements Feature {
    protected static final String keyListenerPriority = "listener_priority";

    protected final int listenerPriority;

    public AbstractFeature(int listenerPriority) {
        this.listenerPriority = listenerPriority;
    }

    public int listenerPriority() { return listenerPriority; }

    /**
     * An abstract feature instance.
     */
    public static abstract class Instance implements Feature.Instance {
        protected final TreeNode parent;

        public Instance(TreeNode parent) {
            this.parent = parent;
        }

        @Override public TreeNode parent() { return parent; }

        protected String lck(String key) {
            return "feature." + base().id() + "." + key;
        }

        protected <S extends Feature.Instance> @Nullable S softDepend(Key<S> key) {
            return parent.system(key).orElse(null);
        }

        protected <S extends Feature.Instance> @Nullable S softDepend(Class<S> type) {
            return parent.system(type).orElse(null);
        }

        protected <S extends Feature.Instance> S depend(Key<S> key) {
            return parent.system(key)
                    .orElseThrow(() -> new IllegalStateException("System [" + base().id() + "] depends on [" + key.id() + "]"));
        }

        protected <S extends Feature.Instance> S depend(Class<S> type) {
            return parent.system(type)
                    .orElseThrow(() -> new IllegalStateException("System [" + base().id() + "] depends on [" + type.getSimpleName() + "]"));
        }

        @Override public String toString() {
            return "<%s>".formatted(base().id());
        }
    }

    @Override public String toString() { return "(%s)".formatted(id()); }
}
