package com.gitlab.aecsocket.sokol.core.impl;

import com.gitlab.aecsocket.minecommons.core.event.Cancellable;
import com.gitlab.aecsocket.sokol.core.*;
import com.gitlab.aecsocket.sokol.core.event.NodeEvent;
import com.gitlab.aecsocket.sokol.core.registry.Keyed;
import com.gitlab.aecsocket.sokol.core.stat.StatIntermediate;
import com.gitlab.aecsocket.sokol.core.wrapper.Item;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractFeature<D, F extends FeatureInstance<D, N>, N extends Node.Scoped<N, I, ?, ?, ?>, I extends Item.Scoped<I, N>>
        implements Feature<F, D> {
    public AbstractFeature() {
        Keyed.validate(id());
    }

    protected abstract SokolPlatform platform();

    public static abstract class AbstractInstance<D, N extends Node.Scoped<N, ?, ?, ?, ?>> implements FeatureInstance<D, N> {
        @Override
        public void build(Tree<N> treeCtx, StatIntermediate stats) {}

        protected String lcKey(String key) {
            return "feature." + type().id() + "." + key;
        }

        protected <E extends NodeEvent<N> & Cancellable> boolean callCancelled(Tree<N> ctx, @Nullable E event) {
            if (event != null)
                return ctx.call(event).cancelled();
            return false;
        }
    }
}
