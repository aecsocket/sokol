package com.gitlab.aecsocket.sokol.core.stat;

/**
 * Combines two values of the same type.
 * @param <T> The value type.
 */
@FunctionalInterface
public interface Combiner<T> {
    /**
     * Combines two values of type {@link T} to make a new value.
     * @param a The first value.
     * @param b The second value.
     * @return The combined value.
     */
    T combine(T a, T b);
}
