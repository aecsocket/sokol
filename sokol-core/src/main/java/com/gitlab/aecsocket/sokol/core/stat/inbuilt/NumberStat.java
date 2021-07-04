package com.gitlab.aecsocket.sokol.core.stat.inbuilt;

import com.gitlab.aecsocket.minecommons.core.CollectionBuilder;
import com.gitlab.aecsocket.sokol.core.stat.BasicStat;
import com.gitlab.aecsocket.sokol.core.stat.Combiner;
import com.gitlab.aecsocket.sokol.core.stat.StatDescriptor;
import io.leangen.geantyref.TypeToken;

import java.util.HashMap;
import java.util.Map;

public final class NumberStat {
    private NumberStat() {}

    public static SInteger ofInt(int def) {
        return new SInteger(def);
    }

    public static SDouble ofDouble(double def) {
        return new SDouble(def);
    }

    public static final class SInteger extends BasicStat<StatDescriptor<Integer>> {
        public static final Map<String, Combiner<Integer>> OPERATIONS = CollectionBuilder.map(new HashMap<String, Combiner<Integer>>())
                .put("=", (a, b) -> b)
                .put("+", (a, b) -> a + b)
                .put("-", (a, b) -> a - b)
                .put("*", (a, b) -> a * b)
                .put("/", (a, b) -> a / b)
                .build();
        public static final String DEFAULT_OPERATOR = "=";

        public SInteger(int defaultValue) {
            super(new TypeToken<StatDescriptor<Integer>>() {},
                    new StatDescriptor<>(defaultValue),
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

        public SDouble(double defaultValue) {
            super(new TypeToken<StatDescriptor<Double>>() {},
                    new StatDescriptor<>(defaultValue),
                    (a, b) -> a.operate(OPERATIONS, DEFAULT_OPERATOR, b),
                    v -> v.copy(i -> i));
        }
    }
}
