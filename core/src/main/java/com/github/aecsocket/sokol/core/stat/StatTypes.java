package com.github.aecsocket.sokol.core.stat;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class StatTypes {
    private static final StatTypes EMPTY = new StatTypes(Collections.emptyMap());

    private final Map<String, Stat<?>> handle;

    public StatTypes(Map<String, Stat<?>> handle) {
        this.handle = Collections.unmodifiableMap(handle);
    }

    public static Builder builder() { return new Builder(); }

    public static StatTypes empty() { return EMPTY; }

    public Map<String, Stat<?>> map() { return handle; }

    public <T> Stat<T> get(String key) {
        @SuppressWarnings("unchecked")
        Stat<T> result = (Stat<T>) handle.get(key);
        return result;
    }

    public static final class Builder {
        private final Map<String, Stat<?>> handle = new HashMap<>();

        private Builder() {}

        public Builder add(Stat<?> value) {
            handle.put(value.key(), value);
            return this;
        }

        public Builder add(Collection<Stat<?>> values) {
            for (var stat : values)
                handle.put(stat.key(), stat);
            return this;
        }

        public Builder add(StatTypes values) {
            handle.putAll(values.map());
            return this;
        }

        public StatTypes build() { return new StatTypes(handle); }
    }
}
