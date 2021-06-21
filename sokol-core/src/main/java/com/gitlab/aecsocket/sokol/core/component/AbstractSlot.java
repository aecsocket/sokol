package com.gitlab.aecsocket.sokol.core.component;

import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.rule.Visitor;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.*;

@ConfigSerializable
public abstract class AbstractSlot<C extends Component> implements Slot {
    protected final Set<String> tags;
    protected final Rule rule;
    protected transient String key;
    protected transient C parent;

    public AbstractSlot(Collection<String> tags, Rule rule, String key, C parent) {
        this.tags = Collections.unmodifiableSet(tags instanceof Set<String> sTags ? sTags : new HashSet<>(tags));
        this.rule = rule;
        this.key = key;
        this.parent = parent;
    }

    public AbstractSlot(Collection<String> tags, Rule rule) {
        this(tags, rule, null, null);
    }

    @Override public Collection<String> tags() { return tags; }
    @Override public boolean tagged(String tag) { return tags.contains(tag); }
    @Override public @NotNull Rule rule() { return rule; }
    @Override @NotNull public String key() { return key; }
    @Override @NotNull public C parent() { return parent; }

    protected abstract @NotNull Class<C> componentType();

    @Override
    public boolean compatible(@Nullable TreeNode parent, @Nullable TreeNode child) {
        if (child == null)
            return true;
        if (!componentType().isInstance(child.value()))
            return false;
        rule.visit(new Visitor.SlotCompatibility(child, parent));
        return rule.applies(child);
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
                ", rule=" + rule +
                '>';
    }
}
