package com.gitlab.aecsocket.sokol.core.registry;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A map of keyed {@link T} objects to their IDs.
 * @param <T> The keyed object type.
 */
public class Registry<T extends Keyed> extends HashMap<String, T> {
    public Registry(int initialCapacity, float loadFactor) { super(initialCapacity, loadFactor); }
    public Registry(int initialCapacity) { super(initialCapacity); }
    public Registry() {}
    public Registry(Map<? extends String, ? extends T> m) { super(m); }

    /**
     * Gets an element by its key.
     * @param key The key.
     * @return An Optional of the result.
     */
    public Optional<T> getOpt(@NotNull String key) { return Optional.ofNullable(get(key)); }

    /**
     * Puts a keyed element in, using its ID as a key.
     * @param obj The object.
     * @throws IllegalArgumentException If the object's ID is invalid.
     */
    public void register(@NotNull T obj) throws IllegalArgumentException {
        if (!Keyed.validKey(obj.id()))
            throw new IllegalArgumentException("Invalid ID [" + obj.id() + "], must be " + Keyed.VALID_KEY);
        put(obj.id(), obj);
    }
}
