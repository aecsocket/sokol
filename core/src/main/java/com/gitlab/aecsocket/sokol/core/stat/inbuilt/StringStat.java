package com.gitlab.aecsocket.sokol.core.stat.inbuilt;

import com.gitlab.aecsocket.sokol.core.stat.BasicStat;
import com.gitlab.aecsocket.sokol.core.stat.Descriptor;
import com.gitlab.aecsocket.sokol.core.stat.Operator;
import com.gitlab.aecsocket.sokol.core.stat.Operators;
import io.leangen.geantyref.TypeToken;

/**
 * A stat type which stores a string.
 */
public final class StringStat extends BasicStat<String> {
    public static final Operator<String> OP_SET = op("=", c -> c.arg(0), String.class);
    public static final Operator<String> OP_ADD = op("+", c -> c.base() + c.arg(0), String.class);

    public static final Operator<String> OP_DEF = OP_SET;
    public static final Operators<String> OPERATORS = Operators.operators(OP_DEF, OP_SET, OP_ADD);

    public static final class Serializer extends Descriptor.Serializer<String> {
        public static final Serializer INSTANCE = new Serializer();
        @Override protected Operators<String> operators() { return OPERATORS; }
    }

    private StringStat() { super(new TypeToken<>() {}, OP_DEF); }
    public static StringStat stringStat() { return new StringStat(); }
}
