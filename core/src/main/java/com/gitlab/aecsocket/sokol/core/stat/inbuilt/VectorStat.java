package com.gitlab.aecsocket.sokol.core.stat.inbuilt;

import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Vector2;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Vector3;
import com.gitlab.aecsocket.sokol.core.stat.BasicStat;
import com.gitlab.aecsocket.sokol.core.stat.Descriptor;
import com.gitlab.aecsocket.sokol.core.stat.Operator;
import com.gitlab.aecsocket.sokol.core.stat.Operators;
import io.leangen.geantyref.TypeToken;

public final class VectorStat {
    private VectorStat() {}

    public static SVector2 vector2Stat() { return new SVector2(); }
    public static SVector3 vector3Stat() { return new SVector3(); }

    public static final class SVector2 extends BasicStat<Vector2> {
        public static final Operator<Vector2> OP_SET = op("=", c -> c.arg(0), Vector2.class);
        public static final Operator<Vector2> OP_ADD = op("+", c -> c.base(Vector2.ZERO).add(c.arg(0)), Vector2.class);
        public static final Operator<Vector2> OP_SUB = op("-", c -> c.base(Vector2.ZERO).subtract(c.arg(0)), Vector2.class);
        public static final Operator<Vector2> OP_MULT = op("*", c -> c.base(Vector2.ZERO).multiply(c.arg(0)), Vector2.class);
        public static final Operator<Vector2> OP_DIV = op("/", c -> c.base(Vector2.ZERO).divide(c.arg(0)), Vector2.class);

        public static final Operator<Vector2> OP_DEF = OP_SET;
        public static final Operators<Vector2> OPERATORS = Operators.operators(OP_DEF,
                OP_SET, OP_ADD, OP_SUB, OP_MULT, OP_DIV);

        public static final class Serializer extends Descriptor.Serializer<Vector2> {
            public static final Serializer INSTANCE = new Serializer();
            @Override protected Operators<Vector2> operators() { return OPERATORS; }
        }

        private SVector2() { super(new TypeToken<>() {}, OP_DEF); }
    }

    public static final class SVector3 extends BasicStat<Vector3> {
        public static final Operator<Vector3> OP_SET = op("=", c -> c.arg(0), Vector3.class);
        public static final Operator<Vector3> OP_ADD = op("+", c -> c.base(Vector3.ZERO).add(c.arg(0)), Vector3.class);
        public static final Operator<Vector3> OP_SUB = op("-", c -> c.base(Vector3.ZERO).subtract(c.arg(0)), Vector3.class);
        public static final Operator<Vector3> OP_MULT = op("*", c -> c.base(Vector3.ZERO).multiply(c.arg(0)), Vector3.class);
        public static final Operator<Vector3> OP_DIV = op("/", c -> c.base(Vector3.ZERO).divide(c.arg(0)), Vector3.class);

        public static final Operator<Vector3> OP_DEF = OP_SET;
        public static final Operators<Vector3> OPERATORS = Operators.operators(OP_DEF,
                OP_SET, OP_ADD, OP_SUB, OP_MULT, OP_DIV);

        public static final class Serializer extends Descriptor.Serializer<Vector3> {
            public static final Serializer INSTANCE = new Serializer();
            @Override protected Operators<Vector3> operators() { return OPERATORS; }
        }

        private SVector3() { super(new TypeToken<>() {}, OP_DEF); }
    }
}
