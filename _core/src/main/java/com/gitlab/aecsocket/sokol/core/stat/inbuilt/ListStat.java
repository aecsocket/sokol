package com.gitlab.aecsocket.sokol.core.stat.inbuilt;

import com.gitlab.aecsocket.sokol.core.stat.Operator;
import com.gitlab.aecsocket.sokol.core.stat.Stat;
import io.leangen.geantyref.TypeToken;

import java.util.*;

import static com.gitlab.aecsocket.sokol.core.stat.Operator.*;

public abstract class ListStat<E> extends Stat<List<E>> {
    public static <E> Operator<List<E>> opSet(TypeToken<List<E>> type) { return op("=", c -> c.arg(0), type); }
    public static <E> Operator<List<E>> opAdd(TypeToken<List<E>> type) {
        return op("+", c -> {
            List<E> result = new ArrayList<>(c.base().orElse(Collections.emptyList()));
            result.addAll(c.arg(0));
            return result;
        }, type);
    }

    public ListStat(String key) {
        super(key);
    }
}
