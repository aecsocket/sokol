package com.gitlab.aecsocket.sokol.core.component;

import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.rule.Visitor;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.*;

/**
 * An abstract slot with default implementations for methods.
 * @param <C> The type component this holds.
 */
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

    @Override public @NotNull Collection<String> tags() { return tags; }
    @Override public boolean tagged(@NotNull String tag) { return tags.contains(tag); }
    @Override public @NotNull Rule rule() { return rule; }
    @Override public @NotNull String key() { return key; }
    @Override public @NotNull C parent() { return parent; }

    /**
     * Gets the type of component this accepts.
     * @return The type.
     */
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

    /**
     * Parents this slot to a component. This should not normally be used, unless a component is being initialized.
     * @param key The key of this slot.
     * @param parent The parent component of this slot.
     */
    public void parent(@NotNull String key, @NotNull C parent) {
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
        return Objects.hash(tags, key);
    }

    @Override
    public String toString() {
        return "<" +
                "tags=" + tags +
                ", rule=" + rule +
                '>';
    }
}
