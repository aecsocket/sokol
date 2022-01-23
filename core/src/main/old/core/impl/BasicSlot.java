package com.gitlab.aecsocket.sokol.core.impl;

import com.gitlab.aecsocket.sokol.core.Component;
import com.gitlab.aecsocket.sokol.core.Slot;
import com.gitlab.aecsocket.sokol.core.rule.RuleException;
import com.gitlab.aecsocket.sokol.core.Node;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class BasicSlot implements Slot {
    protected transient Component parent;
    protected transient String key;
    protected final Set<String> tags;
    protected final Rule rule;

    public BasicSlot(Component parent, String key, Set<String> tags, Rule rule) {
        this.parent = parent;
        this.key = key;
        this.tags = tags;
        this.rule = rule;
    }

    public BasicSlot(Set<String> tags, Rule rule) {
        this.tags = tags;
        this.rule = rule;
    }

    protected BasicSlot() {
        tags = Collections.emptySet();
        rule = Rule.Constant.TRUE;
    }

    @Override public Component parent() { return parent; }
    @Override public String key() { return key; }
    @Override public Set<String> tags() { return new HashSet<>(tags); }
    public Rule rule() { return rule; }

    @Override public boolean tagged(String tag) { return tags.contains(tag); }

    public void parent(Component parent, String key) {
        if (this.parent != null)
            throw new IllegalStateException("Slot already parented");
        this.parent = parent;
        this.key = key;
    }

    @Override
    public <N extends Node.Scoped<N, ?, ?, ?, ?>> void compatibility(N parent, N child) throws RuleException {
        rule.applies(child, parent);
    }
}
