package com.gitlab.aecsocket.sokol.core.stat;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public record Operators<T>(
        Map<String, Operator<T>> operators,
        String def
) {
    public static <T> Operators<T> operators(Map<String, Operator<T>> operators, String def) {
        return new Operators<>(Collections.unmodifiableMap(operators), def);
    }

    public static <T> Operators<T> operators(String def, Collection<Operator<T>> operators) {
        Map<String, Operator<T>> tOperators = new HashMap<>();
        for (var op : operators)
            tOperators.put(op.key(), op);
        return operators(tOperators, def);
    }

    @SafeVarargs
    public static <T> Operators<T> operators(String def, Operator<T>... operators) {
        Map<String, Operator<T>> tOperators = new HashMap<>();
        for (var op : operators)
            tOperators.put(op.key(), op);
        return operators(tOperators, def);
    }

    @SafeVarargs
    public static <T> Operators<T> operators(Operator<T> def, Operator<T>... operators) {
        return operators(def.key(), operators);
    }

    public Operator<T> defOp() { return operators.get(def); }
}
