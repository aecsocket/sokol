package com.gitlab.aecsocket.sokol.core.stat;

import java.util.function.BiFunction;
import java.util.function.Function;

public class BasicStat<T> implements Stat<T> {
    private final Class<T> type;
    private final T defaultValue;
    private final BiFunction<T, T, T> combine;
    private final Function<T, T> copy;

    public BasicStat(Class<T> type, T defaultValue, BiFunction<T, T, T> combine, Function<T, T> copy) {
        this.type = type;
        this.defaultValue = defaultValue;
        this.combine = combine;
        this.copy = copy;
    }

    @Override public Class<T> type() { return type; }
    @Override public T defaultValue() { return defaultValue; }
    @Override public T combine(T a, T b) { return combine.apply(a, b); }
    @Override public T copy(T v) { return copy.apply(v); }

    @Override
    public String toString() {
        return "<%s> (%s)".formatted(type, defaultValue);
    }
}
