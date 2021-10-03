package com.gitlab.aecsocket.sokol.core.stat;

import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public record Operator<T>(String key, Function<Context<T>, T> function, TypeToken<?>[] args) {
    public interface Context<T> {
        Optional<T> base();

        default T reqBase() {
            return base().orElseThrow(() -> new StatOperationException("No base value"));
        }

        <V> V arg(int idx);
    }

    public static <T> Operator<T> op(String key, Function<Context<T>, T> function, TypeToken<?>... args) {
        return new Operator<>(key, function, args);
    }

    public static <T> Operator<T> op(String key, Function<Context<T>, T> function, Class<?>... args) {
        TypeToken<?>[] cArgs = new TypeToken<?>[args.length];
        for (int i = 0; i < args.length; i++)
            cArgs[i] = TypeToken.get(args[i]);
        return new Operator<>(key, function, cArgs);
    }

    @SafeVarargs
    public static <T> Map<String, Operator<T>> ops(Operator<T>... operators) {
        Map<String, Operator<T>> result = new HashMap<>();
        for (var op : operators)
            result.put(op.key, op);
        return result;
    }

    static <T> Context<T> context(@Nullable T base, List<Object> args) {
        Optional<T> optBase = Optional.ofNullable(base);
        return new Context<>() {
            @Override public Optional<T> base() { return optBase; }
            @SuppressWarnings("unchecked")
            @Override public <V> V arg(int idx) { return (V) args.get(idx); }
        };
    }

    public @Nullable T operate(@Nullable T base, List<Object> args) throws StatOperationException {
        return function.apply(context(base, args));
    }
}
