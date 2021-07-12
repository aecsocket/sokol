package com.gitlab.aecsocket.sokol.core.component;

import com.gitlab.aecsocket.sokol.core.stat.StatLists;
import com.gitlab.aecsocket.sokol.core.system.System;

import java.util.*;

/**
 * An abstract component with default implementations for methods.
 * @param <C> The type of this component.
 * @param <S> The slot type.
 * @param <B> The base system type.
 */
public abstract class AbstractComponent<C extends AbstractComponent<C, S, B>, S extends Slot, B extends System> implements Component.Scoped<C, S, B> {
    protected final String id;
    protected final Map<String, S> slots;
    protected final Map<String, B> baseSystems;
    protected final Set<String> tags;
    protected final StatLists stats;

    public AbstractComponent(String id, Map<String, S> slots, Map<String, B> baseSystems, Collection<String> tags, StatLists stats) {
        this.id = id;
        this.slots = new HashMap<>(slots);
        this.baseSystems = new HashMap<>(baseSystems);
        this.tags = new HashSet<>(tags);
        this.stats = new StatLists(stats);
    }

    public AbstractComponent(Component.Scoped<C, S, B> o) {
        this(o.id(), o.slots(), o.baseSystems(), o.tags(), o.stats());
    }

    /**
     * Returns this instance as a {@link C}.
     * @return This instance.
     */
    public abstract C self();

    @Override public String id() { return id; }

    @Override public Map<String, S> slots() { return slots; }
    @Override public Optional<S> slot(String key) { return Optional.ofNullable(slots.get(key)); }

    @Override public Map<String, B> baseSystems() { return baseSystems; }
    @Override public Optional<B> baseSystem(String id) { return Optional.ofNullable(baseSystems.get(id)); }

    @Override public StatLists stats() { return stats; }

    @Override public Set<String> tags() { return tags; }
    @Override public boolean tagged(String tag) { return tags.contains(tag); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractComponent<?, ?, ?> component = (AbstractComponent<?, ?, ?>) o;
        return id.equals(component.id) && slots.equals(component.slots) && baseSystems.equals(component.baseSystems) && tags.equals(component.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, slots, baseSystems, tags);
    }

    @Override
    public String toString() {
        return id + '{' +
                "slots=" + slots +
                ", baseSystems=" + baseSystems.keySet() +
                ", tags=" + tags +
                ", stats=" + stats +
                '}';
    }
}