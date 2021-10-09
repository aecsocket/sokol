package com.gitlab.aecsocket.sokol.core.stat;

import java.util.*;

public final class StatTypes {
    public static final class Builder {
        private final Map<String, Stat<?>> handle = new HashMap<>();

        public Builder add(Stat<?>... stats) {
            for (var stat : stats)
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

    private final Map<String, Stat<?>> handle;

    public StatTypes(Map<String, Stat<?>> handle) {
        this.handle = Collections.unmodifiableMap(handle);
    }

    public static Builder builder() { return new Builder(); }

    public static StatTypes types(Stat<?>... stats) {
        return builder().add(stats).build();
    }

    public Map<String, Stat<?>> handle() { return new HashMap<>(handle); }

    public <T> Stat<T> get(String key) {
        @SuppressWarnings("unchecked")
        Stat<T> result = (Stat<T>) handle.get(key);
        return result;
    }
}
