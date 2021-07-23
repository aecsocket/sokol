package com.gitlab.aecsocket.sokol.paper.stat;

import com.gitlab.aecsocket.minecommons.core.CollectionBuilder;
import com.gitlab.aecsocket.sokol.core.stat.Combiner;
import com.gitlab.aecsocket.sokol.core.stat.Stat;
import com.gitlab.aecsocket.sokol.core.stat.StatDescriptor;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;

import static com.gitlab.aecsocket.sokol.core.stat.StatDescriptor.desc;

public abstract class ListStat<E> implements Stat<StatDescriptor<List<E>>> {
    private static final String defaultOperator = "=";
    private final Map<String, Combiner<List<E>>> operations = CollectionBuilder.map(new HashMap<String, Combiner<List<E>>>())
            .put("=", (a, b) -> b)
            .put("+", (a, b) -> {
                a.addAll(b);
                return a;
            })
            .build();

    private final @Nullable StatDescriptor<List<E>> def;
    private final boolean required;

    public ListStat(@Nullable List<E> def, boolean required) {
        this.def = desc(def);
        this.required = required;
    }

    @Override public @Nullable Optional<StatDescriptor<List<E>>> defaultValue() { return Optional.ofNullable(def); }
    @Override public boolean required() { return required; }

    @Override
    public StatDescriptor<List<E>> combine(StatDescriptor<List<E>> a, StatDescriptor<List<E>> b) {
        return a.operate(operations, defaultOperator, b);
    }

    @Override
    public StatDescriptor<List<E>> copy(StatDescriptor<List<E>> v) {
        return v.copy(ArrayList::new);
    }
}
