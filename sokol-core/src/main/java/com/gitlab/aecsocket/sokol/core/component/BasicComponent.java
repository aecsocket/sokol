package com.gitlab.aecsocket.sokol.core.component;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.objectmapping.meta.NodeKey;

import java.util.*;

public abstract class BasicComponent<C extends BasicComponent<C, S, B>, S extends Slot, B extends System.Base<?>> implements Component.Scoped<C, S, B> {
    protected @NodeKey final String id;
    protected final Map<String, S> slots;
    protected final Map<String, B> baseSystems;
    protected final Set<String> tags;

    public BasicComponent(String id, Map<String, S> slots, Map<String, B> baseSystems, Collection<String> tags) {
        this.id = id;
        this.slots = Collections.unmodifiableMap(slots);
        this.baseSystems = Collections.unmodifiableMap(baseSystems);
        this.tags = Collections.unmodifiableSet(tags instanceof Set<String> sTags ? sTags : new HashSet<>(tags));
    }

    public BasicComponent(Component.Scoped<C, S, B> o) {
        this(o.id(), o.slots(), o.baseSystems(), o.tags());
    }

    @Override @NotNull public String id() { return id; }
    @Override @NotNull public Map<String, S> slots() { return slots; }
    @Override @NotNull public Map<String, B> baseSystems() { return baseSystems; }
    @Override @NotNull public Set<String> tags() { return tags; }
    @Override public boolean tagged(String tag) { return tags.contains(tag); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BasicComponent<?, ?, ?> component = (BasicComponent<?, ?, ?>) o;
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
                '}';
    }
}
