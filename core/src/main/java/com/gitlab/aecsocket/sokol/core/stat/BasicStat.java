package com.gitlab.aecsocket.sokol.core.stat;

import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

/**
 * A basic stat implementation.
 * @param <T> The value type.
 */
public class BasicStat<T> implements Stat<T> {
    private final TypeToken<T> type;
    private final @Nullable T def;
    private final Combiner<T> combiner;
    private final Copier<T> copier;

    public BasicStat(TypeToken<T> type, @Nullable T def, Combiner<T> combiner, Copier<T> copier) {
        this.type = type;
        this.def = def;
        this.combiner = combiner;
        this.copier = copier;
    }

    @Override public TypeToken<T> type() { return type; }
    @Override public @Nullable Optional<T> defaultValue() { return Optional.ofNullable(def); }
    @Override public T combine(T a, T b) { return combiner.combine(a, b); }
    @Override public T copy(T v) { return copier.copy(v); }

    @Override
    public String toString() {
        return "<%s> (%s)".formatted(type, def);
    }
}
