package com.github.aecsocket.sokol.core.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.github.aecsocket.minecommons.core.i18n.I18N;
import com.github.aecsocket.sokol.core.FeatureProfile;
import com.github.aecsocket.sokol.core.NodeSlot;
import com.github.aecsocket.sokol.core.SokolComponent;
import com.github.aecsocket.sokol.core.SokolPlatform;
import com.github.aecsocket.sokol.core.stat.StatIntermediate;

import net.kyori.adventure.text.Component;

public abstract class AbstractComponent<
    C extends AbstractComponent<C, S, P>,
    S extends NodeSlot.Scoped<S, C>,
    P extends FeatureProfile<P, ?, ?>
> implements SokolComponent.Scoped<C, S, P> {
    public static final String I18N_KEY = "component";
    public static final String TAGS = "tags";
    public static final String SLOTS = "slots";
    public static final String FEATURES = "features";
    public static final String SOFT_FEATURES = "soft_features";
    public static final String STATS = "stats";

    protected final String id;
    protected final Set<String> tags;
    protected final Map<String, P> features;
    protected final Map<String, S> slots;
    protected final StatIntermediate stats;

    public AbstractComponent(String id, Set<String> tags, Map<String, P> features, Map<String, S> slots, StatIntermediate stats) {
        this.id = id;
        this.tags = tags;
        this.features = features;
        this.slots = slots;
        this.stats = stats;
    }

    public abstract SokolPlatform platform();

    @Override public String id() { return id; }

    @Override public Set<String> tags() { return new HashSet<>(tags); }
    @Override public boolean tagged(String key) { return tags.contains(key); }

    @Override public Map<String, P> features() { return new HashMap<>(features); }
    @Override public Optional<P> feature(String key) { return Optional.ofNullable(features.get(key)); }

    @Override public Map<String, S> slots() { return new HashMap<>(slots); }
    @Override public Optional<S> slot(String key) { return Optional.ofNullable(slots.get(key)); }

    @Override public StatIntermediate stats() { return stats; }

    @Override
    public Component render(I18N i18n, Locale locale) {
        return i18n.line(locale, I18N_KEY + "." + id + "." + NAME);
    }

    @Override
    public Optional<List<Component>> renderDescription(I18N i18n, Locale locale) {
        return i18n.orLines(locale, I18N_KEY + "." + id + "." + DESCRIPTION);
    }
}
