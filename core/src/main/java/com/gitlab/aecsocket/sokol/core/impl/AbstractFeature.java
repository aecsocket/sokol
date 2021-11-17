package com.gitlab.aecsocket.sokol.core.impl;

import com.gitlab.aecsocket.sokol.core.*;
import com.gitlab.aecsocket.sokol.core.registry.Keyed;

public abstract class AbstractFeature<F extends FeatureInstance<N>, N extends Node.Scoped<N, ?, ?>> implements Feature<F, N> {
    public AbstractFeature() {
        Keyed.validate(id());
    }

    protected abstract SokolPlatform platform();

    public static abstract class AbstractInstance<N extends Node.Scoped<N, ?, ?>> implements FeatureInstance<N> {
        protected final N parent;

        public AbstractInstance(N parent) {
            this.parent = parent;
        }

        @Override public N parent() { return parent; }

        protected TreeData<N> treeData(N node) {
            return node.treeData().orElseThrow(() -> new IllegalStateException("No tree data"));
        }

        protected String lcKey(String key) {
            return "feature." + type().id() + "." + key;
        }
    }
}
