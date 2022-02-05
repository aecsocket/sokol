package com.github.aecsocket.sokol.core.impl;

import java.util.HashSet;
import java.util.Set;

import com.github.aecsocket.sokol.core.*;
import com.github.aecsocket.sokol.core.rule.Rule;

public class BasicNodeSlot<
    S extends BasicNodeSlot<S, C>,
    C extends SokolComponent.Scoped<C, S, ?>
> implements NodeSlot.Scoped<S, C> {
    protected final C parent;
    protected final String key;
    protected final Set<String> tags;
    protected final Rule rule;

    public BasicNodeSlot(C parent, String key, Set<String> tags, Rule rule) {
        this.parent = parent;
        this.key = key;
        this.tags = tags;
        this.rule = rule;
    }

    @Override public C parent() { return parent; }
    @Override public String key() { return key; }

    @Override public Set<String> tags() { return new HashSet<>(tags); }
    @Override public boolean tagged(String key) { return tags.contains(key); }

    @Override
    public boolean required() {
        return tags.contains(REQUIRED);
    }

    @Override
    public <N extends SokolNode> void compatible(N target, N parent) throws IncompatibleException {
        rule.withParent(parent).applies(target);
    }
}
