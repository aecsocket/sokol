package com.gitlab.aecsocket.sokol.core.impl;

import com.gitlab.aecsocket.sokol.core.Feature;
import com.gitlab.aecsocket.sokol.core.FeatureInstance;
import com.gitlab.aecsocket.sokol.core.Node;
import com.gitlab.aecsocket.sokol.core.SokolPlatform;
import net.kyori.adventure.text.Component;

import java.util.Locale;

public abstract class AbstractFeature<F extends FeatureInstance<N>, N extends Node.Scoped<N, ?, ?>> implements Feature<F, N> {
    protected abstract SokolPlatform platform();

    @Override
    public Component name(Locale locale) {
        return platform().lc().safe(locale, "feature." + id());
    }

    public abstract class AbstractInstance implements FeatureInstance<N> {
        protected final N parent;

        public AbstractInstance(N parent) {
            this.parent = parent;
        }

        @Override public N parent() { return parent; }

        @Override public AbstractFeature<F, N> type() { return AbstractFeature.this; }
    }
}
