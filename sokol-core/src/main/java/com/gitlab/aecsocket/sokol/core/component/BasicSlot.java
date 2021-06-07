package com.gitlab.aecsocket.sokol.core.component;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.*;

@ConfigSerializable
public class BasicSlot<C extends Component> implements Slot {
    protected final Set<String> tags;
    protected transient String key;
    protected transient C parent;

    public BasicSlot(Collection<String> tags, String key, C parent) {
        this.tags = Collections.unmodifiableSet(tags instanceof Set<String> sTags ? sTags : new HashSet<>(tags));
        this.key = key;
        this.parent = parent;
    }

    public BasicSlot(Collection<String> tags) {
        this(tags, null, null);
    }

    @Override public Collection<String> tags() { return tags; }
    @Override public boolean tagged(String tag) { return tags.contains(tag); }
    @Override @NotNull public String key() { return key; }
    @Override @NotNull public C parent() { return parent; }

    public void parent(String key, C parent) {
        this.key = key;
        this.parent = parent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BasicSlot<?> basicSlot = (BasicSlot<?>) o;
        return tags.equals(basicSlot.tags) && Objects.equals(key, basicSlot.key) && Objects.equals(parent, basicSlot.parent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tags, key, parent);
    }

    @Override
    public String toString() {
        return "<" +
                "tags=" + tags +
                '>';
    }
}
