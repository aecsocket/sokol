package com.gitlab.aecsocket.sokol.core.impl;

import com.gitlab.aecsocket.sokol.core.Component;
import com.gitlab.aecsocket.sokol.core.Slot;
import com.gitlab.aecsocket.sokol.core.node.IncompatibilityException;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.Node;

import java.util.HashSet;
import java.util.Set;

public class BasicSlot implements Slot {
    protected final Component parent;
    protected final String key;
    protected final Set<String> tags;
    protected final Rule rule;

    public BasicSlot(Component parent, String key, Set<String> tags, Rule rule) {
        this.parent = parent;
        this.key = key;
        this.tags = tags;
        this.rule = rule;
    }

    @Override public Component parent() { return parent; }
    @Override public String key() { return key; }
    @Override public Set<String> tags() { return new HashSet<>(tags); }
    public Rule rule() { return rule; }

    @Override public boolean tagged(String tag) { return tags.contains(tag); }

    @Override
    public void compatibility(Node parent, Node child) throws IncompatibilityException {
        rule.compatibility(child);
    }
}
