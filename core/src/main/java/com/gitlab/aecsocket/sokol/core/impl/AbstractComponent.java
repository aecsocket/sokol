package com.gitlab.aecsocket.sokol.core.impl;

import com.gitlab.aecsocket.sokol.core.*;
import com.gitlab.aecsocket.sokol.core.registry.Keyed;
import com.gitlab.aecsocket.sokol.core.stat.StatIntermediate;

import java.util.*;

public abstract class AbstractComponent<
        C extends AbstractComponent<C, S, F, N>,
        S extends Slot,
        F extends Feature<? extends FeatureInstance<N>, N>,
        N extends Node.Scoped<N, ?, ?>
> implements Component.Scoped<C, S, F, N> {
    protected final String id;
    protected final Set<String> tags;
    protected final Map<String, S> slots;
    protected final Map<String, F> features;
    protected final StatIntermediate stats;

    public AbstractComponent(String id, Set<String> tags, Map<String, S> slots, Map<String, F> features, StatIntermediate stats) {
        Keyed.validate(id);
        this.id = id;
        this.tags = tags;
        this.slots = slots;
        this.features = features;
        this.stats = stats;
    }

    @Override public String id() { return id; }

    @Override public Set<String> tags() { return new HashSet<>(tags); }
    @Override public boolean tagged(String tag) { return tags.contains(tag); }

    @Override public Map<String, S> slots() { return new HashMap<>(slots); }
    @Override public Optional<S> slot(String key) { return Optional.ofNullable(slots.get(key)); }

    @Override public Map<String, F> features() { return new HashMap<>(features); }
    @Override public Optional<F> feature(String key) { return Optional.ofNullable(features.get(key)); }

    @Override public StatIntermediate stats() { return stats; }
}
