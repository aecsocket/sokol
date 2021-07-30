package com.gitlab.aecsocket.sokol.core.stat;

import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.function.Function;

public record Operator<T>(String key, TypeToken<?>[] args, Function<OperatorContext<T>, T> function) {
    public T operate(@Nullable T base, List<Object> args) {
        return function.apply(OperatorContext.context(base, args));
    }
}
