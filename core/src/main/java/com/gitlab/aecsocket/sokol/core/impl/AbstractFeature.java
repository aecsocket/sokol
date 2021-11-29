package com.gitlab.aecsocket.sokol.core.impl;

import com.gitlab.aecsocket.sokol.core.*;
import com.gitlab.aecsocket.sokol.core.registry.Keyed;
import com.gitlab.aecsocket.sokol.core.stat.StatIntermediate;
import com.gitlab.aecsocket.sokol.core.wrapper.Item;

public abstract class AbstractFeature<F extends FeatureInstance<N>, N extends Node.Scoped<N, I, ?, ?>, I extends Item.Scoped<I, N>>
        implements Feature<F, N> {
    public AbstractFeature() {
        Keyed.validate(id());
    }

    protected abstract SokolPlatform platform();

    public static abstract class AbstractInstance<N extends Node.Scoped<N, ?, ?, ?>> implements FeatureInstance<N> {
        protected final N parent;
        protected TreeContext<N> treeCtx;

        public AbstractInstance(N parent) {
            this.parent = parent;
        }

        @Override public N parent() { return parent; }

        @Override
        public void build(TreeContext<N> treeCtx, StatIntermediate stats) {
            this.treeCtx = treeCtx;
        }

        protected String lcKey(String key) {
            return "feature." + type().id() + "." + key;
        }
    }
}
