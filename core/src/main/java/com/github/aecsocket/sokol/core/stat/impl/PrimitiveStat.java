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
        STAT_TYPE_NUMBER_SET = "stat_type.number.set",
        STAT_TYPE_NUMBER_ADD = "stat_type.number.add",
        STAT_TYPE_NUMBER_SUBTRACT = "stat_type.number.subtract",
        STAT_TYPE_NUMBER_MULTIPLY = "stat_type.number.multiply",
        STAT_TYPE_NUMBER_DIVIDE = "stat_type.number.divide";

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

    final class Integer extends Stat<Long> implements PrimitiveStat {
        public record Set(long value) implements Op.Initial<Long>, Op.Discards {
            @Override public Long first() { return value; }
            @Override
            public Component render(I18N i18n, Locale locale) {
                return i18n.line(locale, STAT_TYPE_NUMBER_SET,
                    c -> c.of("value", () -> text(value)));
            }
        }
        public record Add(long value) implements Op.Initial<Long> {
            @Override public Long compute(Long cur) { return cur + value; }
            @Override public Long first() { return compute(0L); }
            @Override
            public Component render(I18N i18n, Locale locale) {
                return i18n.line(locale, STAT_TYPE_NUMBER_ADD,
                    c -> c.of("value", () -> text(value)));
            }
        }
        public record Subtract(long value) implements Op.Initial<Long> {
            @Override public Long compute(Long cur) { return cur - value; }
            @Override public Long first() { return compute(0L); }
            @Override
            public Component render(I18N i18n, Locale locale) {
                return i18n.line(locale, STAT_TYPE_NUMBER_SUBTRACT,
                    c -> c.of("value", () -> text(value)));
            }
        }
        public record Multiply(double value) implements Op.Initial<Long> {
            @Override public Long compute(Long cur) { return (long) (cur * value); }
            @Override public Long first() { return compute(1L); }
            @Override
            public Component render(I18N i18n, Locale locale) {
                return i18n.line(locale, STAT_TYPE_NUMBER_MULTIPLY,
                    c -> c.of("value", () -> text(value)));
            }
        }
        public record Divide(double value) implements Op.Initial<Long> {
            @Override public Long compute(Long cur) { return (long) (cur / value); }
            @Override public Long first() { return compute(0L); }
            @Override
            public Component render(I18N i18n, Locale locale) {
                return i18n.line(locale, STAT_TYPE_NUMBER_DIVIDE,
                    c -> c.of("value", () -> text(value)));
            }
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

    final class Decimal extends Stat<Double> implements PrimitiveStat {
        public record Set(double value) implements Op.Initial<Double>, Op.Discards {
            @Override public Double first() { return value; }
            @Override
            public Component render(I18N i18n, Locale locale) {
                return i18n.line(locale, STAT_TYPE_NUMBER_SET,
                    c -> c.of("value", () -> text(value)));
            }
        }
        public record Add(double value) implements Op.Initial<Double> {
            @Override public Double compute(Double cur) { return cur + value; }
            @Override public Double first() { return compute(0d); }
            @Override
            public Component render(I18N i18n, Locale locale) {
                return i18n.line(locale, STAT_TYPE_NUMBER_ADD,
                    c -> c.of("value", () -> text(value)));
            }
        }
        public record Subtract(double value) implements Op.Initial<Double> {
            @Override public Double compute(Double cur) { return cur - value; }
            @Override public Double first() { return compute(0d); }
            @Override
            public Component render(I18N i18n, Locale locale) {
                return i18n.line(locale, STAT_TYPE_NUMBER_SUBTRACT,
                    c -> c.of("value", () -> text(value)));
            }
        }
        public record Multiply(double value) implements Op.Initial<Double> {
            @Override public Double compute(Double cur) { return cur * value; }
            @Override public Double first() { return compute(1d); }
            @Override
            public Component render(I18N i18n, Locale locale) {
                return i18n.line(locale, STAT_TYPE_NUMBER_MULTIPLY,
                    c -> c.of("value", () -> text(value)));
            }
        }
        public record Divide(double value) implements Op.Initial<Double> {
            @Override public Double compute(Double cur) { return cur / value; }
            @Override public Double first() { return compute(0d); }
            @Override
            public Component render(I18N i18n, Locale locale) {
                return i18n.line(locale, STAT_TYPE_NUMBER_DIVIDE,
                    c -> c.of("value", () -> text(value)));
            }
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
