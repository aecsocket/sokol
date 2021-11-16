package com.gitlab.aecsocket.sokol.core.stat;

import com.gitlab.aecsocket.minecommons.core.translation.Localizer;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;
import java.util.Locale;

import static net.kyori.adventure.text.Component.*;
import static com.gitlab.aecsocket.minecommons.core.serializers.Serializers.*;
import static com.gitlab.aecsocket.sokol.core.Pools.*;

public final class Primitives {
    private Primitives() {}

    public static OfFlag flagStat(String key) { return new OfFlag(key, null); }
    public static OfFlag flagStat(String key, boolean def) { return new OfFlag(key, def); }

    public static class OfFlag extends AbstractStat<Boolean> {
        public record SetValue(boolean value) implements InitialValue<Boolean> {
            @Override public Boolean compute(Boolean cur) { return value; }
            @Override public boolean discardsPrevious() { return true; }
            @Override public Boolean first() { return value; }
            @Override public String toString() { return ""+value; }
            @Override public Component render(Locale locale, Localizer lc) { return lc.safe(locale, "constant." + value).color(CONSTANT); }
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

        public SetValue set(String value) { return new SetValue(value); }
        public AddValue add(String value) { return new AddValue(value); }
    }

    public interface OfNumber<N extends Number> extends Stat<N> {
        interface BaseValue<N extends Number> extends InitialValue<N> {
            Number wrappedValue();
            String operator();
            @Override default boolean discardsPrevious() { return false; }
            default String asString(Locale locale) { return "+" + decimalFormatter(Locale.ROOT).format(wrappedValue()); }
            @Override
            default Component render(Locale locale, Localizer lc) {
                return text(operator(), OPERATOR).append(text(decimalFormatter(locale).format(wrappedValue()), CONSTANT));
            }
        }
        interface SetValue<N extends Number> extends BaseValue<N> {
            @Override default String operator() { return "="; }
            @Override default boolean discardsPrevious() { return true; }
            @Override default String asString(Locale locale) { return decimalFormatter(Locale.ROOT).format(wrappedValue()); }
            @Override
            default Component render(Locale locale, Localizer lc) {
                return text(decimalFormatter(locale).format(wrappedValue()), CONSTANT);
            }
        }
        interface AddValue<N extends Number> extends BaseValue<N> {
            @Override default String operator() { return "+"; }
        }
        interface SubtractValue<N extends Number> extends BaseValue<N> {
            @Override default String operator() { return "-"; }
        }
        interface MultiplyValue<N extends Number> extends BaseValue<N> {
            @Override default String operator() { return "×"; }
        }
        interface DivideValue<N extends Number> extends BaseValue<N> {
            @Override default String operator() { return "÷"; }
        }
    }

    public static OfInteger integerStat(String key) { return new OfInteger(key, null); }
    public static OfInteger integerStat(String key, long def) { return new OfInteger(key, def); }

    public static class OfInteger extends AbstractStat<Long> implements OfNumber<Long> {
        public record SetValue(long value) implements OfNumber.SetValue<Long> {
            @Override public Number wrappedValue() { return value; }
            @Override public Long compute(Long cur) { return value; }
            @Override public Long first() { return value; }
            @Override public String toString() { return asString(Locale.ROOT); }
        }
        public record AddValue(long value) implements OfNumber.AddValue<Long> {
            @Override public Number wrappedValue() { return value; }
            @Override public Long compute(Long cur) { return cur + value; }
            @Override public Long first() { return value; }
            @Override public String toString() { return asString(Locale.ROOT); }
        }
        public record SubtractValue(long value) implements InitialValue<Long>, OfNumber.SubtractValue<Long> {
            @Override public Number wrappedValue() { return value; }
            @Override public Long compute(Long cur) { return cur - value; }
            @Override public Long first() { return value; }
            @Override public String toString() { return asString(Locale.ROOT); }
        }
        public record MultiplyValue(double value) implements InitialValue<Long>, OfNumber.MultiplyValue<Long> {
            @Override public Number wrappedValue() { return value; }
            @Override public Long compute(Long cur) { return (long) (cur * value); }
            @Override public Long first() { return (long) value; }
            @Override public String toString() { return asString(Locale.ROOT); }
        }
        public record DivideValue(double value) implements InitialValue<Long>, OfNumber.DivideValue<Long> {
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

    public static class OfDecimal extends AbstractStat<Double> implements OfNumber<Double> {
        public record SetValue(double value) implements OfNumber.SetValue<Double> {
            @Override public Number wrappedValue() { return value; }
            @Override public Double compute(Double cur) { return value; }
            @Override public Double first() { return value; }
            @Override public String toString() { return asString(Locale.ROOT); }
        }
        public record AddValue(double value) implements OfNumber.AddValue<Double> {
            @Override public Number wrappedValue() { return value; }
            @Override public Double compute(Double cur) { return cur + value; }
            @Override public Double first() { return value; }
            @Override public String toString() { return asString(Locale.ROOT); }
        }
        public record SubtractValue(double value) implements OfNumber.SubtractValue<Double> {
            @Override public Number wrappedValue() { return value; }
            @Override public Double compute(Double cur) { return cur - value; }
            @Override public Double first() { return value; }
            @Override public String toString() { return asString(Locale.ROOT); }
        }
        public record MultiplyValue(double value) implements OfNumber.MultiplyValue<Double> {
            @Override public Number wrappedValue() { return value; }
            @Override public Double compute(Double cur) { return cur * value; }
            @Override public Double first() { return value; }
            @Override public String toString() { return asString(Locale.ROOT); }
        }
        public record DivideValue(double value) implements OfNumber.DivideValue<Double> {
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
