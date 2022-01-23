package com.github.aecsocket.sokol.core.rule.node;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class NodeRuleTypes {
    public static final NodeRuleTypes EMPTY = new NodeRuleTypes(Collections.emptyMap());

    private final Map<String, Class<? extends NodeRule>> handle;

    public NodeRuleTypes(Map<String, Class<? extends NodeRule>> handle) {
        this.handle = Collections.unmodifiableMap(handle);
    }

    public static Builder builder() { return new Builder(); }

    public Map<String, Class<? extends NodeRule>> handle() { return new HashMap<>(handle); }

    public @Nullable Class<? extends NodeRule> get(String key) {
        return handle.get(key);
    }

    public static final class Builder {
        private final Map<String, Class<? extends NodeRule>> handle = new HashMap<>();

        public Builder add(String key, Class<? extends NodeRule> rule) {
            handle.put(key, rule);
            return this;
        }

        public Builder add(Map<String, Class<? extends NodeRule>> rules) {
            handle.putAll(rules);
            return this;
        }

        public Builder add(NodeRuleTypes rules) {
            handle.putAll(rules.handle);
            return this;
        }

        public NodeRuleTypes build() { return new NodeRuleTypes(handle); }
    }
}
