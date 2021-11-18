package com.gitlab.aecsocket.sokol.core.stat;

import com.gitlab.aecsocket.minecommons.core.translation.Localizer;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.NumericalVector;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Vector2;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Vector3;
import com.gitlab.aecsocket.sokol.core.Pools;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;
import java.util.Locale;

import static com.gitlab.aecsocket.minecommons.core.serializers.Serializers.require;
import static com.gitlab.aecsocket.sokol.core.Pools.decimalFormatter;
import static net.kyori.adventure.text.Component.text;

public final class Vectors {
    private Vectors() {}

    public interface OfVector<V extends NumericalVector> extends Stat<V> {
        interface BaseValue<V extends NumericalVector> extends InitialValue<V> {
            V value();
            String operator();
            @Override default boolean discardsPrevious() { return false; }
            default String asString(Locale locale) { return operator() + value().asString(decimalFormatter(locale)); }
            @Override
            default Component render(Locale locale, Localizer lc) {
                return text(operator(), OPERATOR).append(text(value().asString(decimalFormatter(locale)), CONSTANT));
            }
        }
        interface SetValue<V extends NumericalVector> extends BaseValue<V>, Primitives.AbstractNumber.SetValue {
            @Override default String operator() { return "="; }
            @Override default boolean discardsPrevious() { return true; }
            @Override default String asString(Locale locale) { return decimalFormatter(Locale.ROOT).format(value()); }
            @Override
            default Component render(Locale locale, Localizer lc) {
                return text(value().asString(decimalFormatter(locale)), CONSTANT);
            }
        }
        interface AddValue<V extends NumericalVector> extends BaseValue<V>, Primitives.AbstractNumber.AddValue {
            @Override default String operator() { return "+"; }
        }
        interface SubtractValue<V extends NumericalVector> extends BaseValue<V>, Primitives.AbstractNumber.SubtractValue {
            @Override default String operator() { return "-"; }
        }
        interface MultiplyValue<V extends NumericalVector> extends BaseValue<V>, Primitives.AbstractNumber.MultiplyValue {
            @Override default String operator() { return "×"; }
        }
        interface DivideValue<V extends NumericalVector> extends BaseValue<V>, Primitives.AbstractNumber.DivideValue {
            @Override default String operator() { return "÷"; }
        }

        @Override
        default Component renderValue(Locale locale, Localizer lc, V value) {
            return text(value.asString(Pools.decimalFormatter(locale)), CONSTANT);
        }
    }

    public static OfVector2 vector2Stat(String key) { return new OfVector2(key, null); }
    public static OfVector2 vector2Stat(String key, Vector2 def) { return new OfVector2(key, def); }

    public static class OfVector2 extends AbstractStat<Vector2> implements OfVector<Vector2> {
        public interface BaseValue extends OfVector.BaseValue<Vector2> {}
        public record SetValue(Vector2 value) implements BaseValue, OfVector.SetValue<Vector2> {
            @Override public Vector2 compute(Vector2 cur) { return value; }
            @Override public Vector2 first() { return value; }
            @Override public String toString() { return asString(Locale.ROOT); }
        }
        public record AddValue(Vector2 value) implements BaseValue, OfVector.AddValue<Vector2> {
            @Override public Vector2 compute(Vector2 cur) { return cur.add(value); }
            @Override public Vector2 first() { return value; }
            @Override public String toString() { return asString(Locale.ROOT); }
        }
        public record SubtractValue(Vector2 value) implements BaseValue, OfVector.SubtractValue<Vector2> {
            @Override public Vector2 compute(Vector2 cur) { return cur.subtract(value); }
            @Override public Vector2 first() { return value; }
            @Override public String toString() { return asString(Locale.ROOT); }
        }
        public record MultiplyValue(Vector2 value) implements BaseValue, OfVector.MultiplyValue<Vector2> {
            @Override public Vector2 compute(Vector2 cur) { return cur.multiply(value); }
            @Override public Vector2 first() { return value; }
            @Override public String toString() { return asString(Locale.ROOT); }
        }
        public record DivideValue(Vector2 value) implements BaseValue, OfVector.DivideValue<Vector2> {
            @Override public Vector2 compute(Vector2 cur) { return cur.divide(value); }
            @Override public Vector2 first() { return value; }
            @Override public String toString() { return asString(Locale.ROOT); }
        }

        private static final OperationDeserializer<Vector2> opDeserializer = OperationDeserializer.<Vector2>builder()
                .operation("=", (type, node, args) -> new SetValue(require(args[0], Vector2.class)), "value")
                .operation("+", (type, node, args) -> new AddValue(require(args[0], Vector2.class)), "value")
                .operation("-", (type, node, args) -> new SubtractValue(require(args[0], Vector2.class)), "value")
                .operation("*", (type, node, args) -> new MultiplyValue(require(args[0], Vector2.class)), "value")
                .operation("/", (type, node, args) -> new DivideValue(require(args[0], Vector2.class)), "value")
                .defaultOperation("=")
                .build();

        private OfVector2(String key, @Nullable Vector2 defaultValue) {
            super(key, defaultValue);
        }

        @Override
        public Value<Vector2> deserialize(Type type, ConfigurationNode node) throws SerializationException {
            return opDeserializer.deserialize(type, node);
        }

        public SetValue set(Vector2 value) { return new SetValue(value); }
        public AddValue add(Vector2 value) { return new AddValue(value); }
        public SubtractValue subtract(Vector2 value) { return new SubtractValue(value); }
        public MultiplyValue multiply(Vector2 value) { return new MultiplyValue(value); }
        public DivideValue divide(Vector2 value) { return new DivideValue(value); }
    }

    public static OfVector3 vector3Stat(String key) { return new OfVector3(key, null); }
    public static OfVector3 vector3Stat(String key, Vector3 def) { return new OfVector3(key, def); }

    public static class OfVector3 extends AbstractStat<Vector3> implements OfVector<Vector3> {
        public record SetValue(Vector3 value) implements OfVector.SetValue<Vector3> {
            @Override public Vector3 compute(Vector3 cur) { return value; }
            @Override public Vector3 first() { return value; }
            @Override public String toString() { return asString(Locale.ROOT); }
        }
        public record AddValue(Vector3 value) implements OfVector.AddValue<Vector3> {
            @Override public Vector3 compute(Vector3 cur) { return cur.add(value); }
            @Override public Vector3 first() { return value; }
            @Override public String toString() { return asString(Locale.ROOT); }
        }
        public record SubtractValue(Vector3 value) implements OfVector.SubtractValue<Vector3> {
            @Override public Vector3 compute(Vector3 cur) { return cur.subtract(value); }
            @Override public Vector3 first() { return value; }
            @Override public String toString() { return asString(Locale.ROOT); }
        }
        public record MultiplyValue(Vector3 value) implements OfVector.MultiplyValue<Vector3> {
            @Override public Vector3 compute(Vector3 cur) { return cur.multiply(value); }
            @Override public Vector3 first() { return value; }
            @Override public String toString() { return asString(Locale.ROOT); }
        }
        public record DivideValue(Vector3 value) implements OfVector.DivideValue<Vector3> {
            @Override public Vector3 compute(Vector3 cur) { return cur.divide(value); }
            @Override public Vector3 first() { return value; }
            @Override public String toString() { return asString(Locale.ROOT); }
        }

        private static final OperationDeserializer<Vector3> opDeserializer = OperationDeserializer.<Vector3>builder()
                .operation("=", (type, node, args) -> new SetValue(require(args[0], Vector3.class)), "value")
                .operation("+", (type, node, args) -> new AddValue(require(args[0], Vector3.class)), "value")
                .operation("-", (type, node, args) -> new SubtractValue(require(args[0], Vector3.class)), "value")
                .operation("*", (type, node, args) -> new MultiplyValue(require(args[0], Vector3.class)), "value")
                .operation("/", (type, node, args) -> new DivideValue(require(args[0], Vector3.class)), "value")
                .defaultOperation("=")
                .build();

        private OfVector3(String key, @Nullable Vector3 defaultValue) {
            super(key, defaultValue);
        }

        @Override
        public Value<Vector3> deserialize(Type type, ConfigurationNode node) throws SerializationException {
            return opDeserializer.deserialize(type, node);
        }

        public SetValue set(Vector3 value) { return new SetValue(value); }
        public AddValue add(Vector3 value) { return new AddValue(value); }
        public SubtractValue subtract(Vector3 value) { return new SubtractValue(value); }
        public MultiplyValue multiply(Vector3 value) { return new MultiplyValue(value); }
        public DivideValue divide(Vector3 value) { return new DivideValue(value); }
    }
}
