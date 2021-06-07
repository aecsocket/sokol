package com.gitlab.aecsocket.sokol.core.registry;

import java.util.HashMap;
import java.util.Map;

public class Registry<T extends Keyed> extends HashMap<String, T> {
    public Registry(int initialCapacity, float loadFactor) { super(initialCapacity, loadFactor); }
    public Registry(int initialCapacity) { super(initialCapacity); }
    public Registry() {}
    public Registry(Map<? extends String, ? extends T> m) { super(m); }

    public Registry<T> register(T obj) { put(obj.id(), obj); return this; }
}
