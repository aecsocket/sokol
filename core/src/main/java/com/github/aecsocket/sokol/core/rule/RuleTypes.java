package com.github.aecsocket.sokol.core.rule;

import com.github.aecsocket.sokol.core.stat.Stat;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class RuleTypes {
    private static final RuleTypes EMPTY = new RuleTypes(Collections.emptyMap());

    private final Map<String, Class<? extends Rule>> handle;

    public RuleTypes(Map<String, Class<? extends Rule>> handle) {
        this.handle = Collections.unmodifiableMap(handle);
    }

    public static Builder builder() { return new Builder(); }

    public static RuleTypes empty() { return EMPTY; }

    public Map<String, Class<? extends Rule>> map() { return handle; }

    public <T extends Rule> Class<T> get(String key) {
        @SuppressWarnings("unchecked")
        Class<T> result = (Class<T>) handle.get(key);
        return result;
    }

    public static final class Builder {
        private final Map<String, Class<? extends Rule>> handle = new HashMap<>();

        private Builder() {}

        public Builder add(String key, Class<? extends Rule> value) {
            handle.put(key, value);
            return this;
        }

        public Builder add(Map<String, Class<? extends Rule>> values) {
            handle.putAll(values);
            return this;
        }

        public Builder add(RuleTypes values) {
            handle.putAll(values.map());
            return this;
        }

        public RuleTypes build() { return new RuleTypes(handle); }
    }
}
