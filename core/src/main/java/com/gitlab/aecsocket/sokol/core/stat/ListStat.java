package com.gitlab.aecsocket.sokol.core.stat;

import io.leangen.geantyref.TypeToken;

import java.util.*;

public abstract class ListStat<E> extends BasicStat<List<E>> {
    public static <E> Operator<List<E>> opSet(TypeToken<List<E>> type) { return op("=", c -> c.arg(0), type); }
    public static <E> Operator<List<E>> opAdd(TypeToken<List<E>> type) {
        return op("+", c -> {
            List<E> result = new ArrayList<>(c.base(Collections.emptyList()));
            result.addAll(c.arg(0));
            return result;
        }, type);
    }

    public static abstract class Serializer<E> extends Descriptor.Serializer<List<E>> {}

    public ListStat(TypeToken<Descriptor<List<E>>> type, Operator<List<E>> defaultOperator) {
        super(type, defaultOperator);
    }
}
