package com.github.aecsocket.sokol.core.stat;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public interface StatTypes {
    Map<String, Stat<?>> map();

    static StatTypes empty() {
        return EmptyStatTypes.INSTANCE;
    }

    static StatTypes statTypes(Map<String, Stat<?>> map) {
        var copy = Collections.unmodifiableMap(map);
        return () -> copy;
    }

    final class Builder {
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
            handle.putAll(stats.map());
            return this;
        }

        public StatTypes build() { return statTypes(handle); }
    }
}
