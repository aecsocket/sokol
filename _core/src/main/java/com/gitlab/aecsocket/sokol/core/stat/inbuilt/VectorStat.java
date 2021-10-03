package com.gitlab.aecsocket.sokol.core.stat.inbuilt;

import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Vector2;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Vector3;
import com.gitlab.aecsocket.sokol.core.stat.Operator;
import com.gitlab.aecsocket.sokol.core.stat.Stat;

import java.util.Map;

import static com.gitlab.aecsocket.sokol.core.stat.Operator.*;

public final class VectorStat {
    private VectorStat() {}

    public static SVector2 vector2Stat(String key) { return new SVector2(key); }
    public static SVector3 vector3Stat(String key) { return new SVector3(key); }

    public static final class SVector2 extends Stat<Vector2> {
        public static final Operator<Vector2> OP_SET = op("=", c -> c.arg(0), Vector2.class);
        public static final Operator<Vector2> OP_ADD = op("+", c -> c.base().orElse(Vector2.ZERO).add(c.arg(0)), Vector2.class);
        public static final Operator<Vector2> OP_SUB = op("-", c -> c.base().orElse(Vector2.ZERO).subtract(c.arg(0)), Vector2.class);
        public static final Operator<Vector2> OP_MULT = op("*", c -> c.base().orElse(Vector2.ZERO).multiply(c.arg(0)), Vector2.class);
        public static final Operator<Vector2> OP_DIV = op("/", c -> c.base().orElse(Vector2.ZERO).divide(c.arg(0)), Vector2.class);

        public static final Map<String, Operator<Vector2>> OPERATORS = ops(OP_SET, OP_ADD, OP_SUB, OP_MULT, OP_DIV);

        private SVector2(String key) { super(key); }
        @Override public Map<String, Operator<Vector2>> operators() { return OPERATORS; }
        @Override public Operator<Vector2> defaultOperator() { return OP_SET; }
    }

    public static final class SVector3 extends Stat<Vector3> {
        public static final Operator<Vector3> OP_SET = op("=", c -> c.arg(0), Vector3.class);
        public static final Operator<Vector3> OP_ADD = op("+", c -> c.base().orElse(Vector3.ZERO).add(c.arg(0)), Vector3.class);
        public static final Operator<Vector3> OP_SUB = op("-", c -> c.base().orElse(Vector3.ZERO).subtract(c.arg(0)), Vector3.class);
        public static final Operator<Vector3> OP_MULT = op("*", c -> c.base().orElse(Vector3.ZERO).multiply(c.arg(0)), Vector3.class);
        public static final Operator<Vector3> OP_DIV = op("/", c -> c.base().orElse(Vector3.ZERO).divide(c.arg(0)), Vector3.class);

        public static final Map<String, Operator<Vector3>> OPERATORS = ops(OP_SET, OP_ADD, OP_SUB, OP_MULT, OP_DIV);

        private SVector3(String key) { super(key); }
        @Override public Map<String, Operator<Vector3>> operators() { return OPERATORS; }
        @Override public Operator<Vector3> defaultOperator() { return OP_SET; }
    }
}
