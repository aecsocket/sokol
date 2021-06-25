package com.gitlab.aecsocket.sokol.core.stat;

public interface Combiner<T> {
    T combine(T a, T b);
}
