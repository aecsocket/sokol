package com.github.aecsocket.sokol.core.stat;

import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;
import java.util.Locale;

import com.github.aecsocket.minecommons.core.translation.Localizer;

import static com.github.aecsocket.sokol.core.Pools.*;
import static com.gitlab.aecsocket.minecommons.core.serializers.Serializers.*;

public final class Primitives {
    private Primitives() {}

    public static OfFlag flagStat(String key) { return new OfFlag(key, null); }
    public static OfFlag flagStat(String key, boolean def) { return new OfFlag(key, def); }

    public static class OfFlag extends AbstractStat<Boolean> {
        public static String renderConstantKey(boolean value) {
            return "constant." + value;
        }

        public record SetValue(boolean value) implements InitialValue<Boolean> {
            @Override public Boolean compute(Boolean cur) { return value; }
            @Override public boolean discardsPrevious() { return true; }
            @Override public Boolean first() { return value; }
            @Override public String toString() { return ""+value; }
            @Override public Component render(Locale locale, Localizer lc) { return lc.safe(locale, renderConstantKey(value)).color(CONSTANT); }
        }

        private static final Stat.OperationDeserializer<Boolean> opDeserializer = Stat.OperationDeserializer.<Boolean>builder()
                .operation("=", (type, node, args) -> new SetValue(require(args[0], boolean.class)), "value")
                .defaultOperation("=")
                .build();

        private OfFlag(String key, @Nullable Boolean defaultValue) {
            super(key, defaultValue);
        }

        @Override
        public Value<Boolean> deserialize(Type type, ConfigurationNode node) throws SerializationException {
            return opDeserializer.deserialize(type, node);
        }

        @Override
        public Component renderValue(Locale locale, Localizer lc, Boolean value) {
            return lc.safe(locale, renderConstantKey(value)).color(CONSTANT);
        }

        public SetValue set(boolean value) { return new SetValue(value); }
    }

    public static OfString stringStat(String key) { return new OfString(key, null); }
    public static OfString stringStat(String key, String def) { return new OfString(key, def); }

    public static class OfString extends AbstractStat<String> {
        public record SetValue(String value) implements InitialValue<String> {
            @Override public String compute(String cur) { return value; }
            @Override public boolean discardsPrevious() { return true; }
            @Override public String first() { return value; }
            @Override public String toString() { return value; }
            @Override public Component render(Locale locale, Localizer lc) { return text(value, CONSTANT); }
        }
        public record AddValue(String value) implements InitialValue<String> {
            @Override public String compute(String cur) { return cur + value; }
            @Override public boolean discardsPrevious() { return false; }
            @Override public String first() { return value; }
            @Override public String toString() { return "+" + value; }
            @Override public Component render(Locale locale, Localizer lc) { return text("+", OPERATOR).append(text(value, CONSTANT)); }
        }

        private static final Stat.OperationDeserializer<String> opDeserializer = Stat.OperationDeserializer.<String>builder()
                .operation("=", (type, node, args) -> new SetValue(require(args[0], String.class)), "value")
                .operation("+", (type, node, args) -> new AddValue(require(args[0], String.class)), "value")
                .defaultOperation("=")
                .build();

        private OfString(String key, @Nullable String defaultValue) {
            super(key, defaultValue);
        }

        @Override
        public Value<String> deserialize(Type type, ConfigurationNode node) throws SerializationException {
            return opDeserializer.deserialize(type, node);
        }

        @Override
        public Component renderValue(Locale locale, Localizer lc, String value) {
            return text(value, CONSTANT);
        }

        public SetValue set(String value) { return new SetValue(value); }
        public AddValue add(String value) { return new AddValue(value); }
    }

    public interface SingleNumber<N extends Number> extends Stat<N> {
        interface BaseValue<N extends Number> extends InitialValue<N>, NumericalStat.Value {
            Number wrappedValue();
            String operator();
            @Override default boolean discardsPrevious() { return false; }
            default String asString(Locale locale) { return operator() + decimalFormatter(Locale.ROOT).format(wrappedValue()); }
            @Override
            default Component render(Locale locale, Localizer lc) {
                return text(operator(), OPERATOR).append(text(decimalFormatter(locale).format(wrappedValue()), CONSTANT));
            }
        }
        interface SetValue<N extends Number> extends BaseValue<N>, NumericalStat.SetValue {
            @Override default String operator() { return "="; }
            @Override default boolean discardsPrevious() { return true; }
            @Override default String asString(Locale locale) { return decimalFormatter(Locale.ROOT).format(wrappedValue()); }
            @Override
            default Component render(Locale locale, Localizer lc) {
                return text(decimalFormatter(locale).format(wrappedValue()), CONSTANT);
            }
        }
        interface AddValue<N extends Number> extends BaseValue<N>, NumericalStat.AddValue {
            @Override default String operator() { return "+"; }
        }
        interface SubtractValue<N extends Number> extends BaseValue<N>, NumericalStat.SubtractValue {
            @Override default String operator() { return "-"; }
        }
        interface MultiplyValue<N extends Number> extends BaseValue<N>, NumericalStat.MultiplyValue {
            @Override default String operator() { return "×"; }
        }
        interface DivideValue<N extends Number> extends BaseValue<N>, NumericalStat.DivideValue {
            @Override default String operator() { return "÷"; }
        }

        @Override
        default Component renderValue(Locale locale, Localizer lc, N value) {
            return text(Pools.decimalFormatter(locale).format(value), CONSTANT);
        }
    }

    public static OfInteger integerStat(String key) { return new OfInteger(key, null); }
    public static OfInteger integerStat(String key, long def) { return new OfInteger(key, def); }

    public static class OfInteger extends AbstractStat<Long> implements SingleNumber<Long> {
        public record SetValue(long value) implements SingleNumber.SetValue<Long> {
            @Override public Number wrappedValue() { return value; }
            @Override public Long compute(Long cur) { return value; }
            @Override public Long first() { return value; }
            @Override public String toString() { return asString(Locale.ROOT); }
        }
        public record AddValue(long value) implements SingleNumber.AddValue<Long> {
            @Override public Number wrappedValue() { return value; }
            @Override public Long compute(Long cur) { return cur + value; }
            @Override public Long first() { return value; }
            @Override public String toString() { return asString(Locale.ROOT); }
        }
        public record SubtractValue(long value) implements SingleNumber.SubtractValue<Long> {
            @Override public Number wrappedValue() { return value; }
            @Override public Long compute(Long cur) { return cur - value; }
            @Override public Long first() { return value; }
            @Override public String toString() { return asString(Locale.ROOT); }
        }
        public record MultiplyValue(double value) implements SingleNumber.MultiplyValue<Long> {
            @Override public Number wrappedValue() { return value; }
            @Override public Long compute(Long cur) { return (long) (cur * value); }
            @Override public Long first() { return (long) value; }
            @Override public String toString() { return asString(Locale.ROOT); }
        }
        public record DivideValue(double value) implements SingleNumber.DivideValue<Long> {
            @Override public Number wrappedValue() { return value; }
            @Override public Long compute(Long cur) { return (long) (cur / value); }
            @Override public Long first() { return (long) value; }
            @Override public String toString() { return asString(Locale.ROOT); }
        }

        private static final Stat.OperationDeserializer<Long> opDeserializer = Stat.OperationDeserializer.<Long>builder()
                .operation("=", (type, node, args) -> new SetValue(require(args[0], long.class)), "value")
                .operation("+", (type, node, args) -> new AddValue(require(args[0], long.class)), "value")
                .operation("-", (type, node, args) -> new SubtractValue(require(args[0], long.class)), "value")
                .operation("*", (type, node, args) -> new MultiplyValue(require(args[0], double.class)), "value")
                .operation("/", (type, node, args) -> new DivideValue(require(args[0], double.class)), "value")
                .defaultOperation("=")
                .build();

        private OfInteger(String key, @Nullable Long defaultValue) {
            super(key, defaultValue);
        }

        @Override
        public Value<Long> deserialize(Type type, ConfigurationNode node) throws SerializationException {
            return opDeserializer.deserialize(type, node);
        }

        public SetValue set(long value) { return new SetValue(value); }
        public AddValue add(long value) { return new AddValue(value); }
        public SubtractValue subtract(long value) { return new SubtractValue(value); }
        public MultiplyValue multiply(long value) { return new MultiplyValue(value); }
        public DivideValue divide(long value) { return new DivideValue(value); }
    }

    public static OfDecimal decimalStat(String key) { return new OfDecimal(key, null); }
    public static OfDecimal decimalStat(String key, double def) { return new OfDecimal(key, def); }

    public static class OfDecimal extends AbstractStat<Double> implements SingleNumber<Double> {
        public record SetValue(double value) implements SingleNumber.SetValue<Double> {
            @Override public Number wrappedValue() { return value; }
            @Override public Double compute(Double cur) { return value; }
            @Override public Double first() { return value; }
            @Override public String toString() { return asString(Locale.ROOT); }
        }
        public record AddValue(double value) implements SingleNumber.AddValue<Double> {
            @Override public Number wrappedValue() { return value; }
            @Override public Double compute(Double cur) { return cur + value; }
            @Override public Double first() { return value; }
            @Override public String toString() { return asString(Locale.ROOT); }
        }
        public record SubtractValue(double value) implements SingleNumber.SubtractValue<Double> {
            @Override public Number wrappedValue() { return value; }
            @Override public Double compute(Double cur) { return cur - value; }
            @Override public Double first() { return value; }
            @Override public String toString() { return asString(Locale.ROOT); }
        }
        public record MultiplyValue(double value) implements SingleNumber.MultiplyValue<Double> {
            @Override public Number wrappedValue() { return value; }
            @Override public Double compute(Double cur) { return cur * value; }
            @Override public Double first() { return value; }
            @Override public String toString() { return asString(Locale.ROOT); }
        }
        public record DivideValue(double value) implements SingleNumber.DivideValue<Double> {
            @Override public Number wrappedValue() { return value; }
            @Override public Double compute(Double cur) { return cur / value; }
            @Override public Double first() { return value; }
            @Override public String toString() { return asString(Locale.ROOT); }
        }

        private static final Stat.OperationDeserializer<Double> opDeserializer = Stat.OperationDeserializer.<Double>builder()
                .operation("=", (type, node, args) -> new SetValue(require(args[0], double.class)), "value")
                .operation("+", (type, node, args) -> new AddValue(require(args[0], double.class)), "value")
                .operation("-", (type, node, args) -> new SubtractValue(require(args[0], double.class)), "value")
                .operation("*", (type, node, args) -> new MultiplyValue(require(args[0], double.class)), "value")
                .operation("/", (type, node, args) -> new DivideValue(require(args[0], double.class)), "value")
                .defaultOperation("=")
                .build();

        private OfDecimal(String key, @Nullable Double defaultValue) {
            super(key, defaultValue);
        }

        @Override
        public Value<Double> deserialize(Type type, ConfigurationNode node) throws SerializationException {
            return opDeserializer.deserialize(type, node);
        }

        public SetValue set(double value) { return new SetValue(value); }
        public AddValue add(double value) { return new AddValue(value); }
        public SubtractValue subtract(double value) { return new SubtractValue(value); }
        public MultiplyValue multiply(double value) { return new MultiplyValue(value); }
        public DivideValue divide(double value) { return new DivideValue(value); }
    }
}
