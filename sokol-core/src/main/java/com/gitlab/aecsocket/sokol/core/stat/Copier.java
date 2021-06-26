package com.gitlab.aecsocket.sokol.core.stat;

/**
 * Makes a copy of a value.
 * @param <T> The type of value.
 */
@FunctionalInterface
public interface Copier<T> {
    /**
     * Copies a value of type {@link T}.
     * @param v The value,
     * @return The copy.
     */
    T copy(T v);
}
