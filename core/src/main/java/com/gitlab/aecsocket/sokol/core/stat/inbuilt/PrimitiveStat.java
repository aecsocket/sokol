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

public final class PrimitiveStat {
    private PrimitiveStat() {}

    public static SBoolean booleanStat(@Nullable Boolean def) { return new SBoolean(def, false); }
    public static SBoolean booleanStat() { return new SBoolean(null, true); }

    public static SInteger intStat(@Nullable Integer def) { return new SInteger(def, false); }
    public static SInteger intStat() { return new SInteger(null, true); }

    public static SLong longStat(@Nullable Long def) { return new SLong(def, false); }
    public static SLong longStat() { return new SLong(null, true); }

    public static SFloat floatStat(@Nullable Float def) { return new SFloat(def, false); }
    public static SFloat floatStat() { return new SFloat(null, true); }

    public static SDouble doubleStat(@Nullable Double def) { return new SDouble(def, false); }
    public static SDouble doubleStat() { return new SDouble(null, true); }

    public static final class SBoolean extends BasicStat<Boolean> {
        private SBoolean(@Nullable Boolean def, boolean required) {
            super(new TypeToken<Boolean>() {},
                    def,
                    required,
                    (a, b) -> b,
                    v -> v);
        }
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

        private SInteger(@Nullable Integer def, boolean required) {
            super(new TypeToken<StatDescriptor<Integer>>() {},
                    desc(def),
                    required,
                    (a, b) -> a.operate(OPERATIONS, DEFAULT_OPERATOR, b),
                    v -> v.copy(i -> i));
        }
    }

    public static final class SLong extends BasicStat<StatDescriptor<Long>> {
        public static final Map<String, Combiner<Long>> OPERATIONS = CollectionBuilder.map(new HashMap<String, Combiner<Long>>())
                .put("=", (a, b) -> b)
                .put("+", (a, b) -> a + b)
                .put("-", (a, b) -> a - b)
                .put("*", (a, b) -> a * b)
                .put("/", (a, b) -> a / b)
                .build();
        public static final String DEFAULT_OPERATOR = "=";

        private SLong(@Nullable Long def, boolean required) {
            super(new TypeToken<StatDescriptor<Long>>() {},
                    desc(def),
                    required,
                    (a, b) -> a.operate(OPERATIONS, DEFAULT_OPERATOR, b),
                    v -> v.copy(i -> i));
        }
    }

    public static final class SFloat extends BasicStat<StatDescriptor<Float>> {
        public static final Map<String, Combiner<Float>> OPERATIONS = CollectionBuilder.map(new HashMap<String, Combiner<Float>>())
                .put("=", (a, b) -> b)
                .put("+", (a, b) -> a + b)
                .put("-", (a, b) -> a - b)
                .put("*", (a, b) -> a * b)
                .put("/", (a, b) -> a / b)
                .build();
        public static final String DEFAULT_OPERATOR = "=";

        private SFloat(@Nullable Float def, boolean required) {
            super(new TypeToken<StatDescriptor<Float>>() {},
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
