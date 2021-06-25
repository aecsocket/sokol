package com.gitlab.aecsocket.sokol.core.stat;

public interface Copier<T> {
    T copy(T v);
}
