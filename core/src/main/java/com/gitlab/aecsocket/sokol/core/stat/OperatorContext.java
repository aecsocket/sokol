package com.gitlab.aecsocket.sokol.core.stat;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

public interface OperatorContext<T> {
    @Nullable T base();

    <V> V arg(int idx);

    default T base(T def) {
        T val = base();
        return val == null ? def : val;
    }

    static <T> OperatorContext<T> context(@Nullable T base, List<Object> args) {
        return new OperatorContext<>() {
            @Override public @Nullable T base() { return base; }
            @SuppressWarnings("unchecked")
            @Override public <V> V arg(int idx) { return (V) args.get(idx); }
        };
    }
}
