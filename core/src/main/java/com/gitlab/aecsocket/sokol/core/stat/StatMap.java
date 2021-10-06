package com.gitlab.aecsocket.sokol.core.stat;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class StatMap extends HashMap<String, Stat.Node<?>> {
    public StatMap(int initialCapacity, float loadFactor) { super(initialCapacity, loadFactor); }
    public StatMap(int initialCapacity) { super(initialCapacity); }
    public StatMap() {}
    public StatMap(Map<? extends String, ? extends Stat.Node<?>> m) { super(m); }

    public <T> Optional<Stat.Node<T>> node(String key) {
        @SuppressWarnings("unchecked")
        Stat.Node<T> node = (Stat.Node<T>) get(key);
        return Optional.ofNullable(node);
    }

    public <T> Optional<Stat.Node<T>> node(Stat<T> key) {
        return node(key.key());
    }

    public <T> Optional<T> value(String key) {
        @SuppressWarnings("unchecked")
        Stat.Node<T> node = (Stat.Node<T>) get(key);
        return node == null ? Optional.empty() : Optional.ofNullable(node.compute());
    }

    public <T> Optional<T> value(Stat<T> key) {
        @SuppressWarnings("unchecked")
        Stat.Node<T> node = (Stat.Node<T>) get(key);
        return node == null ? key.defaultValue() : Optional.ofNullable(node.compute());
    }

    public <T> T required(Stat<T> key) throws IllegalStateException {
        return value(key)
                .orElseThrow(() -> new IllegalStateException("No value for stat [" + key.key() + "]"));
    }
}
