package com.github.aecsocket.sokol.core.stat.impl;

import com.github.aecsocket.minecommons.core.i18n.I18N;
import com.github.aecsocket.sokol.core.SokolPlatform;
import com.github.aecsocket.sokol.core.stat.Stat;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Locale;

import static com.github.aecsocket.minecommons.core.serializers.Serializers.require;
import static net.kyori.adventure.text.Component.text;

public interface PrimitiveStat {
    String
        STAT_TYPE_NUMBER = "stat_type.number.";

    final class Flag extends Stat<Boolean> implements PrimitiveStat {
        public record Set(boolean value) implements Op.Initial<Boolean>, Op.Discards {
            @Override public Boolean first() { return value; }
            @Override
            public Component render(I18N i18n, Locale locale) {
                return i18n.line(locale, STAT_TYPE_FLAG_SET,
                    c -> c.of("value", () -> c.line(value ? SokolPlatform.YES : SokolPlatform.NO)));
            }
        }

        public static final String
            STAT_TYPE_FLAG_SET = "stat_type.flag.set";
        private static final OpTypes<Boolean> OP_TYPES = Stat.<Boolean>buildOpTypes()
            .setDefault("=", (type, node, args) -> new Set(require(args[0], boolean.class)), "value")
            .build();

        private Flag(String key, @Nullable Boolean def) {
            super(key, def);
        }

        @Override public OpTypes<Boolean> opTypes() { return OP_TYPES; }

        @Override
        public Component renderValue(I18N i18n, Locale locale, Boolean value) {
            return Component.text(value);
        }
    }

    static Flag flag(String key, boolean def) {
        return new Flag(key, def);
    }

    static Flag flag(String key) {
        return new Flag(key, null);
    }

    interface OfNumber extends PrimitiveStat {
        interface NumberOp<T extends Number> extends Stat.Op.Initial<T> {
            double asDouble();
            String name();

            @Override
            default Component render(I18N i18n, Locale locale) {
                return i18n.line(locale, STAT_TYPE_NUMBER + name(),
                        c -> c.of("value", () -> text(asDouble())));
            }
        }
        interface SetOp<T extends Number> extends NumberOp<T>, Stat.Op.Discards {
            @Override default String name() { return "set"; }
        }

        interface SumOp<T extends Number> extends NumberOp<T> {}
        interface AddOp<T extends Number> extends SumOp<T> {
            @Override default String name() { return "add"; }
        }
        interface SubtractOp<T extends Number> extends SumOp<T> {
            @Override default String name() { return "subtract"; }
        }

        interface FactorOp<T extends Number> extends NumberOp<T> {}
        interface MultiplyOp<T extends Number> extends FactorOp<T> {
            @Override default String name() { return "multiply"; }
        }
        interface DivideOp<T extends Number> extends FactorOp<T> {
            @Override default String name() { return "divide"; }
        }
    }

    final class Integer extends Stat<Long> implements OfNumber {
        public record Set(long value) implements SetOp<Long> {
            @Override public Long first() { return value; }
            @Override public double asDouble() { return value; }
        }
        public record Add(long value) implements AddOp<Long> {
            @Override public Long compute(Long cur) { return cur + value; }
            @Override public Long first() { return compute(0L); }
            @Override public double asDouble() { return value; }
        }
        public record Subtract(long value) implements SubtractOp<Long> {
            @Override public Long compute(Long cur) { return cur - value; }
            @Override public Long first() { return compute(0L); }
            @Override public double asDouble() { return value; }
        }
        public record Multiply(double value) implements MultiplyOp<Long> {
            @Override public Long compute(Long cur) { return (long) (cur * value); }
            @Override public Long first() { return compute(1L); }
            @Override public double asDouble() { return value; }
        }
        public record Divide(double value) implements DivideOp<Long> {
            @Override public Long compute(Long cur) { return (long) (cur / value); }
            @Override public Long first() { return compute(0L); }
            @Override public double asDouble() { return value; }
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

        @Override
        public Component renderValue(I18N i18n, Locale locale, Long value) {
            return Component.text(value);
        }
    }

    static Integer integer(String key, long def) {
        return new Integer(key, def);
    }

    static Integer integer(String key) {
        return new Integer(key, null);
    }

    final class Decimal extends Stat<Double> implements OfNumber {
        public record Set(Double value) implements SetOp<Double> {
            @Override public Double first() { return value; }
            @Override public double asDouble() { return value; }
        }
        public record Add(Double value) implements AddOp<Double> {
            @Override public Double compute(Double cur) { return cur + value; }
            @Override public Double first() { return compute(0d); }
            @Override public double asDouble() { return value; }
        }
        public record Subtract(Double value) implements SubtractOp<Double> {
            @Override public Double compute(Double cur) { return cur - value; }
            @Override public Double first() { return compute(0d); }
            @Override public double asDouble() { return value; }
        }
        public record Multiply(double value) implements MultiplyOp<Double> {
            @Override public Double compute(Double cur) { return cur * value; }
            @Override public Double first() { return compute(1d); }
            @Override public double asDouble() { return value; }
        }
        public record Divide(double value) implements DivideOp<Double> {
            @Override public Double compute(Double cur) { return cur / value; }
            @Override public Double first() { return compute(0d); }
            @Override public double asDouble() { return value; }
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

        @Override
        public Component renderValue(I18N i18n, Locale locale, Double value) {
            return Component.text(value);
        }
    }

    static Decimal decimal(String key, double def) {
        return new Decimal(key, def);
    }

    static Decimal decimal(String key) {
        return new Decimal(key, null);
    }
}
