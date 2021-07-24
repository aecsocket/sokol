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

    public static SBoolean booleanStat(@Nullable Boolean def) { return new SBoolean(def); }
    public static SBoolean booleanStat() { return new SBoolean(null); }

    public static SInteger intStat(@Nullable Integer def) { return new SInteger(def); }
    public static SInteger intStat() { return new SInteger(null); }

    public static SLong longStat(@Nullable Long def) { return new SLong(def); }
    public static SLong longStat() { return new SLong(null); }

    public static SFloat floatStat(@Nullable Float def) { return new SFloat(def); }
    public static SFloat floatStat() { return new SFloat(null); }

    public static SDouble doubleStat(@Nullable Double def) { return new SDouble(def); }
    public static SDouble doubleStat() { return new SDouble(null); }

    public static final class SBoolean extends BasicStat<Boolean> {
        private SBoolean(@Nullable Boolean def) {
            super(new TypeToken<>() {},
                    def,
                    (a, b) -> b,
                    v -> v);
        }
    }

    public static final class SInteger extends BasicStat<StatDescriptor<Integer>> {
        @SuppressWarnings("Convert2MethodRef")
        public static final Map<String, Combiner<Integer>> OPERATIONS = CollectionBuilder.map(new HashMap<String, Combiner<Integer>>())
                .put("=", (a, b) -> b)
                .put("+", (a, b) -> a + b)
                .put("-", (a, b) -> a - b)
                .put("*", (a, b) -> a * b)
                .put("/", (a, b) -> a / b)
                .build();
        public static final String DEFAULT_OPERATOR = "=";

        private SInteger(@Nullable Integer def) {
            super(new TypeToken<>() {},
                    desc(def),
                    (a, b) -> a.operate(OPERATIONS, DEFAULT_OPERATOR, b),
                    v -> v.copy(i -> i));
        }
    }

    public static final class SLong extends BasicStat<StatDescriptor<Long>> {
        @SuppressWarnings("Convert2MethodRef")
        public static final Map<String, Combiner<Long>> OPERATIONS = CollectionBuilder.map(new HashMap<String, Combiner<Long>>())
                .put("=", (a, b) -> b)
                .put("+", (a, b) -> a + b)
                .put("-", (a, b) -> a - b)
                .put("*", (a, b) -> a * b)
                .put("/", (a, b) -> a / b)
                .build();
        public static final String DEFAULT_OPERATOR = "=";

        private SLong(@Nullable Long def) {
            super(new TypeToken<>() {},
                    desc(def),
                    (a, b) -> a.operate(OPERATIONS, DEFAULT_OPERATOR, b),
                    v -> v.copy(i -> i));
        }
    }

    public static final class SFloat extends BasicStat<StatDescriptor<Float>> {
        @SuppressWarnings("Convert2MethodRef")
        public static final Map<String, Combiner<Float>> OPERATIONS = CollectionBuilder.map(new HashMap<String, Combiner<Float>>())
                .put("=", (a, b) -> b)
                .put("+", (a, b) -> a + b)
                .put("-", (a, b) -> a - b)
                .put("*", (a, b) -> a * b)
                .put("/", (a, b) -> a / b)
                .build();
        public static final String DEFAULT_OPERATOR = "=";

        private SFloat(@Nullable Float def) {
            super(new TypeToken<>() {},
                    desc(def),
                    (a, b) -> a.operate(OPERATIONS, DEFAULT_OPERATOR, b),
                    v -> v.copy(i -> i));
        }
    }

    public static final class SDouble extends BasicStat<StatDescriptor<Double>> {
        @SuppressWarnings("Convert2MethodRef")
        public static final Map<String, Combiner<Double>> OPERATIONS = CollectionBuilder.map(new HashMap<String, Combiner<Double>>())
                .put("=", (a, b) -> b)
                .put("+", (a, b) -> a + b)
                .put("-", (a, b) -> a - b)
                .put("*", (a, b) -> a * b)
                .put("/", (a, b) -> a / b)
                .build();
        public static final String DEFAULT_OPERATOR = "=";

        private SDouble(@Nullable Double def) {
            super(new TypeToken<>() {},
                    desc(def),
                    (a, b) -> a.operate(OPERATIONS, DEFAULT_OPERATOR, b),
                    v -> v.copy(i -> i));
        }
    }
}
