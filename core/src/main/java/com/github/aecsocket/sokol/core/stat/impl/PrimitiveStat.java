package com.github.aecsocket.sokol.core.stat.impl;

import com.github.aecsocket.sokol.core.stat.Stat;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.github.aecsocket.minecommons.core.serializers.Serializers.require;

public interface PrimitiveStat {
    final class Flag extends Stat<Boolean> implements PrimitiveStat {
        public record Set(boolean val) implements Op.Initial<Boolean>, Op.Discards {
            @Override public Boolean first() { return val; }
        }

        private static final OpTypes<Boolean> OP_TYPES = Stat.<Boolean>buildOpTypes()
            .setDefault("=", (type, node, args) -> new Set(require(args[0], boolean.class)), "value")
            .build();

        private Flag(String key, @Nullable Boolean def) {
            super(key, def);
        }

        @Override public OpTypes<Boolean> opTypes() { return OP_TYPES; }
    }

    static Flag flag(String key, boolean def) {
        return new Flag(key, def);
    }

    static Flag flag(String key) {
        return new Flag(key, null);
    }

    final class Integer extends Stat<Long> implements PrimitiveStat {
        public record Set(long val) implements Op.Initial<Long>, Op.Discards {
            @Override public Long first() { return val; }
        }
        public record Add(long val) implements Op.Initial<Long> {
            @Override public Long compute(Long cur) { return cur + val; }
            @Override public Long first() { return compute(0L); }
        }
        public record Subtract(long val) implements Op.Initial<Long> {
            @Override public Long compute(Long cur) { return cur - val; }
            @Override public Long first() { return compute(0L); }
        }
        public record Multiply(double val) implements Op.Initial<Long> {
            @Override public Long compute(Long cur) { return (long) (cur + val); }
            @Override public Long first() { return compute(0L); }
        }
        public record Divide(double val) implements Op.Initial<Long> {
            @Override public Long compute(Long cur) { return (long) (cur + val); }
            @Override public Long first() { return compute(0L); }
        }

        private static final OpTypes<Long> OP_TYPES = Stat.<Long>buildOpTypes()
                .setDefault("=", (type, node, args) -> new Set(require(args[0], long.class)), "value")
                .set("+", (type, node, args) -> new Add(require(args[0], long.class)), "value")
                .set("-", (type, node, args) -> new Subtract(require(args[0], long.class)), "value")
                .set("*", (type, node, args) -> new Multiply(require(args[0], double.class)), "value")
                .set("/", (type, node, args) -> new Divide(require(args[0], double.class)), "value")
                .build();

        public Integer(String key, @Nullable Long def) {
            super(key, def);
        }

        @Override public OpTypes<Long> opTypes() { return OP_TYPES; }
    }

    static Integer integer(String key, long def) {
        return new Integer(key, def);
    }

    static Integer integer(String key) {
        return new Integer(key, null);
    }

    final class Decimal extends Stat<Double> implements PrimitiveStat {
        public record Set(double val) implements Op.Initial<Double>, Op.Discards {
            @Override public Double first() { return val; }
        }
        public record Add(double val) implements Op.Initial<Double> {
            @Override public Double compute(Double cur) { return cur + val; }
            @Override public Double first() { return compute(0d); }
        }
        public record Subtract(double val) implements Op.Initial<Double> {
            @Override public Double compute(Double cur) { return cur - val; }
            @Override public Double first() { return compute(0d); }
        }
        public record Multiply(double val) implements Op.Initial<Double> {
            @Override public Double compute(Double cur) { return cur + val; }
            @Override public Double first() { return compute(0d); }
        }
        public record Divide(double val) implements Op.Initial<Double> {
            @Override public Double compute(Double cur) { return cur + val; }
            @Override public Double first() { return compute(0d); }
        }

        private static final OpTypes<Double> OP_TYPES = Stat.<Double>buildOpTypes()
                .setDefault("=", (type, node, args) -> new Set(require(args[0], double.class)), "value")
                .set("+", (type, node, args) -> new Add(require(args[0], double.class)), "value")
                .set("-", (type, node, args) -> new Subtract(require(args[0], double.class)), "value")
                .set("*", (type, node, args) -> new Multiply(require(args[0], double.class)), "value")
                .set("/", (type, node, args) -> new Divide(require(args[0], double.class)), "value")
                .build();

        public Decimal(String key, @Nullable Double def) {
            super(key, def);
        }

        @Override public OpTypes<Double> opTypes() { return OP_TYPES; }
    }

    static Decimal decimal(String key, double def) {
        return new Decimal(key, def);
    }

    static Decimal decimal(String key) {
        return new Decimal(key, null);
    }
}
