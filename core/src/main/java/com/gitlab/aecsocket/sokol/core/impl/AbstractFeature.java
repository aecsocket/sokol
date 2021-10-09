package com.gitlab.aecsocket.sokol.core.impl;

import com.gitlab.aecsocket.sokol.core.Feature;
import com.gitlab.aecsocket.sokol.core.FeatureInstance;
import com.gitlab.aecsocket.sokol.core.Node;
import com.gitlab.aecsocket.sokol.core.SokolPlatform;
import com.gitlab.aecsocket.sokol.core.registry.Keyed;

public abstract class AbstractFeature<F extends FeatureInstance<N>, N extends Node.Scoped<N, ?, ?>> implements Feature<F, N> {
    protected abstract SokolPlatform platform();

    public AbstractFeature() {
        Keyed.validate(id());
    }

    public static abstract class AbstractInstance<N extends Node.Scoped<N, ?, ?>> implements FeatureInstance<N> {
        protected final N parent;

        public AbstractInstance(N parent) {
            this.parent = parent;
        }

        @Override public N parent() { return parent; }
    }
}
