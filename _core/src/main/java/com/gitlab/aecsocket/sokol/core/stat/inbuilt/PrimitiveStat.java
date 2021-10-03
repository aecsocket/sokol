package com.gitlab.aecsocket.sokol.core.stat.inbuilt;

import com.gitlab.aecsocket.sokol.core.stat.Operator;
import com.gitlab.aecsocket.sokol.core.stat.Stat;

import java.util.Map;
import java.util.function.DoubleFunction;

import static com.gitlab.aecsocket.sokol.core.stat.Operator.*;

public final class PrimitiveStat {
    private PrimitiveStat() {}

    public static SBoolean booleanStat(String key) { return new SBoolean(key); }
    public static SInteger intStat(String key) { return new SInteger(key); }
    public static SLong longStat(String key) { return new SLong(key); }
    public static SFloat floatStat(String key) { return new SFloat(key); }
    public static SDouble doubleStat(String key) { return new SDouble(key); }

    public static final class SBoolean extends Stat<Boolean> {
        public static final Operator<Boolean> OP_SET = op("=", c -> c.arg(0), boolean.class);

        public static final Map<String, Operator<Boolean>> OPERATORS = ops(OP_SET);

        private SBoolean(String key) { super(key); }
        @Override public Map<String, Operator<Boolean>> operators() { return OPERATORS; }
        @Override public Operator<Boolean> defaultOperator() { return OP_SET; }
    }

    private interface NumberOperator {
        double operate(double a, double b);
    }

    private static <N extends Number> Operator<N> nOp(Class<N> nType, String key, N def, DoubleFunction<N> converter, NumberOperator op) {
        return op(key, c -> converter.apply(op.operate(c.base().orElse(def).doubleValue(), c.<N>arg(0).doubleValue())), nType);
    }
    private static <N extends Number> Operator<N> nSet(Class<N> nType) {
        return op("=", c -> c.arg(0), nType);
    }
    @SuppressWarnings("Convert2MethodRef")
    private static <N extends Number> Operator<N> nAdd(Class<N> nType, N def, DoubleFunction<N> converter) {
        return nOp(nType, "+", def, converter, (a, b) -> a + b);
    }
    private static <N extends Number> Operator<N> nSub(Class<N> nType, N def, DoubleFunction<N> converter) {
        return nOp(nType, "-", def, converter, (a, b) -> a - b);
    }
    private static <N extends Number> Operator<N> nMult(Class<N> nType, N def, DoubleFunction<N> converter) {
        return nOp(nType, "*", def, converter, (a, b) -> a * b);
    }
    private static <N extends Number> Operator<N> nDiv(Class<N> nType, N def, DoubleFunction<N> converter) {
        return nOp(nType, "/", def, converter, (a, b) -> a / b);
    }

    public static final class SInteger extends Stat<Integer> {
        public static final Operator<Integer> OP_SET = nSet(int.class);
        public static final Operator<Integer> OP_ADD = nAdd(int.class, 0, n -> (int) n);
        public static final Operator<Integer> OP_SUB = nSub(int.class, 0, n -> (int) n);
        public static final Operator<Integer> OP_MULT = nMult(int.class, 0, n -> (int) n);
        public static final Operator<Integer> OP_DIV = nDiv(int.class, 0, n -> (int) n);

        public static final Map<String, Operator<Integer>> OPERATORS = ops(OP_SET, OP_ADD, OP_SUB, OP_MULT, OP_DIV);

        private SInteger(String key) { super(key); }
        @Override public Map<String, Operator<Integer>> operators() { return OPERATORS; }
        @Override public Operator<Integer> defaultOperator() { return OP_SET; }
    }

    public static final class SLong extends Stat<Long> {
        public static final Operator<Long> OP_SET = nSet(long.class);
        public static final Operator<Long> OP_ADD = nAdd(long.class, 0L, n -> (long) n);
        public static final Operator<Long> OP_SUB = nSub(long.class, 0L, n -> (long) n);
        public static final Operator<Long> OP_MULT = nMult(long.class, 0L, n -> (long) n);
        public static final Operator<Long> OP_DIV = nDiv(long.class, 0L, n -> (long) n);

        public static final Map<String, Operator<Long>> OPERATORS = ops(OP_SET, OP_ADD, OP_SUB, OP_MULT, OP_DIV);

        private SLong(String key) { super(key); }
        @Override public Map<String, Operator<Long>> operators() { return OPERATORS; }
        @Override public Operator<Long> defaultOperator() { return OP_SET; }
    }

    public static final class SFloat extends Stat<Float> {
        public static final Operator<Float> OP_SET = nSet(float.class);
        public static final Operator<Float> OP_ADD = nAdd(float.class, 0f, n -> (float) n);
        public static final Operator<Float> OP_SUB = nSub(float.class, 0f, n -> (float) n);
        public static final Operator<Float> OP_MULT = nMult(float.class, 0f, n -> (float) n);
        public static final Operator<Float> OP_DIV = nDiv(float.class, 0f, n -> (float) n);

        public static final Map<String, Operator<Float>> OPERATORS = ops(OP_SET, OP_ADD, OP_SUB, OP_MULT, OP_DIV);

        private SFloat(String key) { super(key); }
        @Override public Map<String, Operator<Float>> operators() { return OPERATORS; }
        @Override public Operator<Float> defaultOperator() { return OP_SET; }
    }

    public static final class SDouble extends Stat<Double> {
        public static final Operator<Double> OP_SET = nSet(double.class);
        public static final Operator<Double> OP_ADD = nAdd(double.class, 0d, n -> n);
        public static final Operator<Double> OP_SUB = nSub(double.class, 0d, n -> n);
        public static final Operator<Double> OP_MULT = nMult(double.class, 0d, n -> n);
        public static final Operator<Double> OP_DIV = nDiv(double.class, 0d, n -> n);

        public static final Map<String, Operator<Double>> OPERATORS = ops(OP_SET, OP_ADD, OP_SUB, OP_MULT, OP_DIV);

        private SDouble(String key) { super(key); }
        @Override public Map<String, Operator<Double>> operators() { return OPERATORS; }
        @Override public Operator<Double> defaultOperator() { return OP_SET; }
    }
}
