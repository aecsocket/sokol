package com.github.aecsocket.sokol.core.impl;

import java.util.HashSet;
import java.util.Set;

import com.github.aecsocket.sokol.core.api.*;
import com.github.aecsocket.sokol.core.rule.base.BaseRule;

public class AbstractNodeSlot implements NodeSlot {
    protected final Component parent;
    protected final String key;
    protected final Set<String> tags;
    protected final BaseRule rule;

    public AbstractNodeSlot(Component parent, String key, Set<String> tags, BaseRule rule) {
        this.parent = parent;
        this.key = key;
        this.tags = tags;
        this.rule = rule;
    }

    @Override public Component parent() { return parent; }
    @Override public String key() { return key; }

    @Override public Set<String> tags() { return new HashSet<>(tags); }
    @Override public boolean tagged(String tag) { return tags.contains(tag); }

    @Override
    public <N extends BaseNode.Scoped<N, ?>> void compatible(N target, N parent) throws IncompatibleException {
        rule.applies(target, parent);
    }
}
