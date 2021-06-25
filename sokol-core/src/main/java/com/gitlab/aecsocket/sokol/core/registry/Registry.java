package com.gitlab.aecsocket.sokol.core.registry;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Registry<T extends Keyed> extends HashMap<String, T> {
    public Registry(int initialCapacity, float loadFactor) { super(initialCapacity, loadFactor); }
    public Registry(int initialCapacity) { super(initialCapacity); }
    public Registry() {}
    public Registry(Map<? extends String, ? extends T> m) { super(m); }

    public Optional<T> getOpt(String key) { return Optional.ofNullable(get(key)); }

    public void register(T obj) {
        if (!Keyed.validKey(obj.id()))
            throw new IllegalArgumentException("Invalid ID [" + obj.id() + "], must be " + Keyed.VALID_KEY);
        put(obj.id(), obj);
    }
}
