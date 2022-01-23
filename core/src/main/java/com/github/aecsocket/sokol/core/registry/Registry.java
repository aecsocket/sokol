package com.github.aecsocket.sokol.core.registry;

import java.util.*;

public final class Registry<T extends Keyed> {
    private final Map<String, T> registry = new HashMap<>();

    public Map<String, T> registry() { return new HashMap<>(registry); }

    public Set<String> keySet() { return registry.keySet(); }

    public Collection<T> values() { return registry.values(); }

    public int size() { return registry.size(); }

    public Optional<T> get(String id) {
        return Optional.ofNullable(registry.get(id));
    }

    public void register(T val) throws ValidationException {
        String id = val.id();
        if (registry.containsKey(id))
            throw new IllegalArgumentException("Value with ID `" + id + "` already exists");
        Keyed.validate(id);
        registry.put(id, val);
    }

    public void registerAll(Registry<T> o) {
        registry.putAll(o.registry);
    }

    public void unregister(String id) {
        registry.remove(id);
    }

    public void unregisterAll() { registry.clear(); }
}
