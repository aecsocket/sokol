package com.gitlab.aecsocket.sokol.core.stat.collection;

import com.gitlab.aecsocket.sokol.core.stat.Stat;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class StatTypes {
    public static final class Builder {
        private final StatTypes handle = new StatTypes();

        public Builder add(Stat<?>... val) { handle.putAll(val); return this; }
        public Builder add(Collection<Stat<?>> val) { handle.putAll(val); return this; }
        public Builder add(StatTypes val) { handle.putAll(val); return this; }

        public StatTypes build() { return handle; }
    }

    public static Builder builder() { return new Builder(); }

    private static final StatTypes empty = new StatTypes(Collections.emptyMap());

    public static StatTypes empty() { return empty; }

    private final Map<String, Stat<?>> handle;

    public StatTypes(Map<? extends String, ? extends Stat<?>> handle) {
        this.handle = new HashMap<>(handle);
    }

    public StatTypes() {
        handle = new HashMap<>();
    }

    public static StatTypes of(Stat<?>... stats) {
        return builder().add(stats).build();
    }

    public <T> Stat<T> get(String key) {
        @SuppressWarnings("unchecked")
        Stat<T> result = (Stat<T>) handle.get(key);
        return result;
    }

    public StatTypes put(Stat<?> stat) {
        handle.put(stat.key(), stat);
        return this;
    }

    public StatTypes define(Stat.Node<?> node) {
        put(node.stat());
        return this;
    }

    public StatTypes putAll(Stat<?>... stats) {
        for (var stat : stats)
            put(stat);
        return this;
    }

    public StatTypes defineAll(Stat.Node<?>... nodes) {
        for (var node : nodes)
            define(node);
        return this;
    }

    public StatTypes putAll(Collection<Stat<?>> stats) {
        for (var stat : stats)
            put(stat);
        return this;
    }

    public StatTypes putAll(StatTypes stats) {
        handle.putAll(stats.handle);
        return this;
    }

    public StatTypes defineAll(Collection<Stat.Node<?>> nodes) {
        for (var node : nodes)
            define(node);
        return this;
    }
}
