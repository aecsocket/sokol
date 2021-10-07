package com.gitlab.aecsocket.sokol.core.impl;

import com.gitlab.aecsocket.sokol.core.*;

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
    protected final Map<String, F> featureTypes;

    public AbstractComponent(String id, Set<String> tags, Map<String, S> slots, Map<String, F> featureTypes) {
        this.id = id;
        this.tags = tags;
        this.slots = slots;
        this.featureTypes = featureTypes;
    }

    protected abstract SokolPlatform platform();

    @Override public String id() { return id; }
    @Override
    public net.kyori.adventure.text.Component name(Locale locale) {
        return platform().lc().safe(locale, "component." + id);
    }

    @Override public Set<String> tags() { return new HashSet<>(tags); }
    @Override public boolean tagged(String tag) { return tags.contains(tag); }

    @Override public Map<String, S> slots() { return new HashMap<>(slots); }
    @Override public Optional<S> slot(String key) { return Optional.ofNullable(slots.get(key)); }

    @Override public Map<String, F> featureTypes() { return new HashMap<>(featureTypes); }
    @Override public Optional<F> featureType(String key) { return Optional.ofNullable(featureTypes.get(key)); }
}
