package com.gitlab.aecsocket.sokol.core.stat.inbuilt;

import com.gitlab.aecsocket.minecommons.core.CollectionBuilder;
import com.gitlab.aecsocket.sokol.core.stat.BasicStat;
import com.gitlab.aecsocket.sokol.core.stat.Combiner;
import com.gitlab.aecsocket.sokol.core.stat.StatDescriptor;
import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;

import static com.gitlab.aecsocket.sokol.core.stat.StatDescriptor.desc;

public final class NumberStat {
    private NumberStat() {}

    public static SInteger intStat(@Nullable Integer def) { return new SInteger(def, false); }
    public static SInteger intStat() { return new SInteger(null, true); }

    public static SDouble doubleStat(@Nullable Double def) { return new SDouble(def, false); }
    public static SDouble doubleStat() { return new SDouble(null, true); }

    public static final class SInteger extends BasicStat<StatDescriptor<Integer>> {
        public static final Map<String, Combiner<Integer>> OPERATIONS = CollectionBuilder.map(new HashMap<String, Combiner<Integer>>())
                .put("=", (a, b) -> b)
                .put("+", (a, b) -> a + b)
                .put("-", (a, b) -> a - b)
                .put("*", (a, b) -> a * b)
                .put("/", (a, b) -> a / b)
                .build();
        public static final String DEFAULT_OPERATOR = "=";

        private SInteger(@Nullable Integer def, boolean required) {
            super(new TypeToken<StatDescriptor<Integer>>() {},
                    desc(def),
                    required,
                    (a, b) -> a.operate(OPERATIONS, DEFAULT_OPERATOR, b),
                    v -> v.copy(i -> i));
        }
    }

    public static final class SDouble extends BasicStat<StatDescriptor<Double>> {
        public static final Map<String, Combiner<Double>> OPERATIONS = CollectionBuilder.map(new HashMap<String, Combiner<Double>>())
                .put("=", (a, b) -> b)
                .put("+", (a, b) -> a + b)
                .put("-", (a, b) -> a - b)
                .put("*", (a, b) -> a * b)
                .put("/", (a, b) -> a / b)
                .build();
        public static final String DEFAULT_OPERATOR = "=";

        private SDouble(@Nullable Double def, boolean required) {
            super(new TypeToken<StatDescriptor<Double>>() {},
                    desc(def),
                    required,
                    (a, b) -> a.operate(OPERATIONS, DEFAULT_OPERATOR, b),
                    v -> v.copy(i -> i));
        }
    }
}
