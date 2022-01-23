package com.github.aecsocket.sokol.core.stat;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;

public final class StatTypes {
    public static final StatTypes EMPTY = new StatTypes(Collections.emptyMap());

    private final Map<String, Stat<?>> handle;

    public StatTypes(Map<String, Stat<?>> handle) {
        this.handle = Collections.unmodifiableMap(handle);
    }

    public static Builder builder() { return new Builder(); }

    public Map<String, Stat<?>> handle() { return new HashMap<>(handle); }

    public @Nullable Stat<?> get(String key) {
        return handle.get(key);
    }

    public static final class Builder {
        private final Map<String, Stat<?>> handle = new HashMap<>();

        public Builder add(Stat<?> stat) {
            handle.put(stat.key(), stat);
            return this;
        }

        public Builder add(Collection<Stat<?>> stats) {
            for (var stat : stats)
                handle.put(stat.key(), stat);
            return this;
        }

        public Builder add(StatTypes stats) {
            handle.putAll(stats.handle);
            return this;
        }

        public StatTypes build() { return new StatTypes(handle); }
    }
}
