package com.gitlab.aecsocket.sokol.core.component;

import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.*;

@ConfigSerializable
public abstract class AbstractSlot<C extends Component> implements Slot {
    protected final Set<String> tags;
    protected final Set<String> accepts;
    protected transient String key;
    protected transient C parent;

    public AbstractSlot(Collection<String> tags, Collection<String> accepts, String key, C parent) {
        this.tags = Collections.unmodifiableSet(tags instanceof Set<String> sTags ? sTags : new HashSet<>(tags));
        this.accepts = Collections.unmodifiableSet(accepts instanceof Set<String> sAccepts ? sAccepts : new HashSet<>(accepts));
        this.key = key;
        this.parent = parent;
    }

    public AbstractSlot(Collection<String> tags, Collection<String> accepts) {
        this(tags, accepts, null, null);
    }

    @Override public Collection<String> tags() { return tags; }
    @Override public boolean tagged(String tag) { return tags.contains(tag); }
    @Override public Set<String> accepts() { return accepts; }
    @Override @NotNull public String key() { return key; }
    @Override @NotNull public C parent() { return parent; }

    protected abstract @NotNull Class<C> componentType();

    @Override
    public boolean compatible(@Nullable TreeNode node) {
        if (node == null)
            return true;
        if (!componentType().isInstance(node.value()))
            return false;
        return accepts.size() == 0 || !Collections.disjoint(node.value().tags(), accepts);
    }

    public void parent(String key, C parent) {
        this.key = key;
        this.parent = parent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractSlot<?> basicSlot = (AbstractSlot<?>) o;
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
                ", accepts=" + accepts +
                '>';
    }
}
