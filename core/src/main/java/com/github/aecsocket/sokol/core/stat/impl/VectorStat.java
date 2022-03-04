package com.github.aecsocket.sokol.core.stat.impl;

import com.github.aecsocket.minecommons.core.DecimalFormats;
import com.github.aecsocket.minecommons.core.i18n.I18N;
import com.github.aecsocket.minecommons.core.vector.cartesian.Vector2;
import com.github.aecsocket.minecommons.core.vector.cartesian.Vector3;
import com.github.aecsocket.sokol.core.stat.Stat;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Locale;

import static com.github.aecsocket.minecommons.core.serializers.Serializers.require;
import static net.kyori.adventure.text.Component.text;

public interface VectorStat {
    String
        STAT_TYPE_VECTOR = "stat_type.vector.";

    interface VectorOp<T> extends Stat.Op.Initial<T> {
        T value();
        String name();

        @Override
        default Component render(I18N i18n, Locale locale) {
            return i18n.line(locale, STAT_TYPE_VECTOR + name(),
                c -> c.of("value", () -> text(""+value())));
        }
    }
    interface SetOp<T> extends VectorOp<T>, Stat.Op.Discards {
        @Override default String name() { return "set"; }
    }

    interface SumOp<T> extends VectorOp<T> {}
    interface AddOp<T> extends SumOp<T> {
        @Override default String name() { return "add"; }
    }
    interface SubtractOp<T> extends SumOp<T> {
        @Override default String name() { return "subtract"; }
    }

    interface FactorOp<T> extends VectorOp<T> {}
    interface MultiplyOp<T> extends FactorOp<T> {
        @Override default String name() { return "multiply"; }
    }
    interface DivideOp<T> extends FactorOp<T> {
        @Override default String name() { return "divide"; }
    }

    final class Of2 extends Stat<Vector2> implements VectorStat {
        public record Set(Vector2 value) implements SetOp<Vector2> {
            @Override public Vector2 first() { return value; }
        }
        public record Add(Vector2 value) implements AddOp<Vector2> {
            @Override public Vector2 compute(Vector2 cur) { return cur.add(value); }
            @Override public Vector2 first() { return compute(Vector2.ZERO); }
        }
        public record Subtract(Vector2 value) implements SubtractOp<Vector2> {
            @Override public Vector2 compute(Vector2 cur) { return cur.subtract(value); }
            @Override public Vector2 first() { return compute(Vector2.ZERO); }
        }
        public record Multiply(Vector2 value) implements MultiplyOp<Vector2> {
            @Override public Vector2 compute(Vector2 cur) { return cur.multiply(value); }
            @Override public Vector2 first() { return compute(Vector2.ONE); }
        }
        public record Divide(Vector2 value) implements DivideOp<Vector2> {
            @Override public Vector2 compute(Vector2 cur) { return cur.divide(value); }
            @Override public Vector2 first() { return compute(Vector2.ONE); }
        }

        private static final OpTypes<Vector2> OP_TYPES = Stat.<Vector2>buildOpTypes()
            .setDefault("=", (type, node, args) -> new Set(require(args[0], Vector2.class)), "value")
            .set("+", (type, node, args) -> new Add(require(args[0], Vector2.class)), "value")
            .set("-", (type, node, args) -> new Subtract(require(args[0], Vector2.class)), "value")
            .set("*", (type, node, args) -> new Multiply(require(args[0], Vector2.class)), "value")
            .set("/", (type, node, args) -> new Divide(require(args[0], Vector2.class)), "value")
            .build();

        public Of2(String key, @Nullable Vector2 def) {
            super(key, def);
        }

        @Override public OpTypes<Vector2> opTypes() { return OP_TYPES; }

        @Override
        public Component renderValue(I18N i18n, Locale locale, Vector2 value) {
            return Component.text(value.asString(DecimalFormats.formatter(locale)));
        }
    }

    static Of2 vec2(String key, Vector2 def) {
        return new Of2(key, def);
    }

    static Of2 vec2(String key) {
        return new Of2(key, null);
    }

    final class Of3 extends Stat<Vector3> implements VectorStat {
        public record Set(Vector3 value) implements SetOp<Vector3> {
            @Override public Vector3 first() { return value; }
        }
        public record Add(Vector3 value) implements AddOp<Vector3> {
            @Override public Vector3 compute(Vector3 cur) { return cur.add(value); }
            @Override public Vector3 first() { return compute(Vector3.ZERO); }
        }
        public record Subtract(Vector3 value) implements SubtractOp<Vector3> {
            @Override public Vector3 compute(Vector3 cur) { return cur.subtract(value); }
            @Override public Vector3 first() { return compute(Vector3.ZERO); }
        }
        public record Multiply(Vector3 value) implements MultiplyOp<Vector3> {
            @Override public Vector3 compute(Vector3 cur) { return cur.multiply(value); }
            @Override public Vector3 first() { return compute(Vector3.ONE); }
        }
        public record Divide(Vector3 value) implements DivideOp<Vector3> {
            @Override public Vector3 compute(Vector3 cur) { return cur.divide(value); }
            @Override public Vector3 first() { return compute(Vector3.ONE); }
        }

        private static final OpTypes<Vector3> OP_TYPES = Stat.<Vector3>buildOpTypes()
            .setDefault("=", (type, node, args) -> new Set(require(args[0], Vector3.class)), "value")
            .set("+", (type, node, args) -> new Add(require(args[0], Vector3.class)), "value")
            .set("-", (type, node, args) -> new Subtract(require(args[0], Vector3.class)), "value")
            .set("*", (type, node, args) -> new Multiply(require(args[0], Vector3.class)), "value")
            .set("/", (type, node, args) -> new Divide(require(args[0], Vector3.class)), "value")
            .build();

        public Of3(String key, @Nullable Vector3 def) {
            super(key, def);
        }

        @Override public OpTypes<Vector3> opTypes() { return OP_TYPES; }

        @Override
        public Component renderValue(I18N i18n, Locale locale, Vector3 value) {
            return Component.text(value.asString(DecimalFormats.formatter(locale)));
        }
    }

    static Of3 vec3(String key, Vector3 def) {
        return new Of3(key, def);
    }

    static Of3 vec3(String key) {
        return new Of3(key, null);
    }
}
