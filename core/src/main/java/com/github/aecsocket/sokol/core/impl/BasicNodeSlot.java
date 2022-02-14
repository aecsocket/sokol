package com.github.aecsocket.sokol.core.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.github.aecsocket.sokol.core.*;
import com.github.aecsocket.sokol.core.rule.Rule;
import org.spongepowered.configurate.objectmapping.meta.Required;

public class BasicNodeSlot<
    S extends BasicNodeSlot<S, C>,
    C extends SokolComponent.Scoped<C, S, ?>
> implements NodeSlot.Scoped<S, C> {
    protected transient C parent;
    protected transient String key;
    protected final Set<String> tags;
    @Required protected final Rule rule;

    public BasicNodeSlot(C parent, String key, Set<String> tags, Rule rule) {
        this.parent = parent;
        this.key = key;
        this.tags = tags;
        this.rule = rule;
    }

    protected BasicNodeSlot() {
        tags = Collections.emptySet();
        rule = null;
    }

    @Override public C parent() { return parent; }
    @Override public String key() { return key; }

    @Override public Set<String> tags() { return new HashSet<>(tags); }
    @Override public boolean tagged(String key) { return tags.contains(key); }
    @Override public boolean required() { return tags.contains(REQUIRED); }
    @Override public boolean modifiable() { return tags.contains(MODIFIABLE); }

    public Rule rule() { return rule; }

    @Override
    public <N extends SokolNode> void compatible(N target, N parent) throws IncompatibleException {
        rule.withParent(parent).applies(target);
    }

    protected void setUp(C parent, String key) {
        if (this.parent != null)
            throw new IllegalStateException("Slot is already set up");
        this.parent = parent;
        this.key = key;
    }
}
