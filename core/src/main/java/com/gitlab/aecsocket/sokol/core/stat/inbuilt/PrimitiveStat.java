package com.gitlab.aecsocket.sokol.core.stat.inbuilt;

import com.gitlab.aecsocket.sokol.core.stat.BasicStat;
import com.gitlab.aecsocket.sokol.core.stat.Descriptor;
import com.gitlab.aecsocket.sokol.core.stat.Operator;
import com.gitlab.aecsocket.sokol.core.stat.Operators;
import io.leangen.geantyref.TypeToken;

import java.util.function.DoubleFunction;

public final class PrimitiveStat {
    private PrimitiveStat() {}

    public static SBoolean booleanStat() { return new SBoolean(); }
    public static SInteger intStat() { return new SInteger(); }
    public static SLong longStat() { return new SLong(); }
    public static SFloat floatStat() { return new SFloat(); }
    public static SDouble doubleStat() { return new SDouble(); }

    public static final class SBoolean extends BasicStat<Boolean> {
        public static final Operator<Boolean> OP_SET = op("=", c -> c.arg(0), boolean.class);

        public static final Operator<Boolean> OP_DEF = OP_SET;
        public static final Operators<Boolean> OPERATORS = Operators.operators(OP_DEF, OP_SET);

        public static final class Serializer extends Descriptor.Serializer<Boolean> {
            public static final Serializer INSTANCE = new Serializer();
            @Override protected Operators<Boolean> operators() { return OPERATORS; }
        }

        private SBoolean() { super(new TypeToken<>() {}, OP_DEF); }
    }

    private interface NumberOperator {
        double operate(double a, double b);
    }

    private static <N extends Number> Operator<N> nOp(Class<N> nType, String key, N def, DoubleFunction<N> converter, NumberOperator op) {
        return BasicStat.op(key, c -> converter.apply(op.operate(c.base(def).doubleValue(), c.<N>arg(0).doubleValue())), nType);
    }
    private static <N extends Number> Operator<N> nSet(Class<N> nType) {
        return BasicStat.op("=", c -> c.arg(0), nType);
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

    public static final class SInteger extends BasicStat<Integer> {
        public static final Operator<Integer> OP_SET = nSet(int.class);
        public static final Operator<Integer> OP_ADD = nAdd(int.class, 0, n -> (int) n);
        public static final Operator<Integer> OP_SUB = nSub(int.class, 0, n -> (int) n);
        public static final Operator<Integer> OP_MULT = nMult(int.class, 0, n -> (int) n);
        public static final Operator<Integer> OP_DIV = nDiv(int.class, 0, n -> (int) n);

        public static final Operator<Integer> OP_DEF = OP_SET;
        public static final Operators<Integer> OPERATORS = Operators.operators(OP_DEF,
                OP_SET, OP_ADD, OP_SUB, OP_MULT, OP_DIV);

        public static final class Serializer extends Descriptor.Serializer<Integer> {
            public static final Serializer INSTANCE = new Serializer();
            @Override protected Operators<Integer> operators() { return OPERATORS; }
        }

        private SInteger() { super(new TypeToken<>() {}, OP_DEF); }
    }

    public static final class SLong extends BasicStat<Long> {
        public static final Operator<Long> OP_SET = nSet(long.class);
        public static final Operator<Long> OP_ADD = nAdd(long.class, 0L, n -> (long) n);
        public static final Operator<Long> OP_SUB = nSub(long.class, 0L, n -> (long) n);
        public static final Operator<Long> OP_MULT = nMult(long.class, 0L, n -> (long) n);
        public static final Operator<Long> OP_DIV = nDiv(long.class, 0L, n -> (long) n);

        public static final Operator<Long> OP_DEF = OP_SET;
        public static final Operators<Long> OPERATORS = Operators.operators(OP_DEF,
                OP_SET, OP_ADD, OP_SUB, OP_MULT, OP_DIV);

        public static final class Serializer extends Descriptor.Serializer<Long> {
            public static final Serializer INSTANCE = new Serializer();
            @Override protected Operators<Long> operators() { return OPERATORS; }
        }

        private SLong() { super(new TypeToken<>() {}, OP_DEF); }
    }

    public static final class SFloat extends BasicStat<Float> {
        public static final Operator<Float> OP_SET = nSet(float.class);
        public static final Operator<Float> OP_ADD = nAdd(float.class, 0f, n -> (float) n);
        public static final Operator<Float> OP_SUB = nSub(float.class, 0f, n -> (float) n);
        public static final Operator<Float> OP_MULT = nMult(float.class, 0f, n -> (float) n);
        public static final Operator<Float> OP_DIV = nDiv(float.class, 0f, n -> (float) n);

        public static final Operator<Float> OP_DEF = OP_SET;
        public static final Operators<Float> OPERATORS = Operators.operators(OP_DEF,
                OP_SET, OP_ADD, OP_SUB, OP_MULT, OP_DIV);

        public static final class Serializer extends Descriptor.Serializer<Float> {
            public static final Serializer INSTANCE = new Serializer();
            @Override protected Operators<Float> operators() { return OPERATORS; }
        }

        private SFloat() { super(new TypeToken<>() {}, OP_DEF); }
    }

    public static final class SDouble extends BasicStat<Double> {
        public static final Operator<Double> OP_SET = nSet(double.class);
        public static final Operator<Double> OP_ADD = nAdd(double.class, 0d, n -> n);
        public static final Operator<Double> OP_SUB = nSub(double.class, 0d, n -> n);
        public static final Operator<Double> OP_MULT = nMult(double.class, 0d, n -> n);
        public static final Operator<Double> OP_DIV = nDiv(double.class, 0d, n -> n);

        public static final Operator<Double> OP_DEF = OP_SET;
        public static final Operators<Double> OPERATORS = Operators.operators(OP_DEF,
                OP_SET, OP_ADD, OP_SUB, OP_MULT, OP_DIV);

        public static final class Serializer extends Descriptor.Serializer<Double> {
            public static final Serializer INSTANCE = new Serializer();
            @Override protected Operators<Double> operators() { return OPERATORS; }
        }

        private SDouble() { super(new TypeToken<>() {}, OP_DEF); }
    }
}
