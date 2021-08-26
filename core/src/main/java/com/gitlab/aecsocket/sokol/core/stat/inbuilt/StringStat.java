package com.gitlab.aecsocket.sokol.core.stat.inbuilt;

import com.gitlab.aecsocket.sokol.core.stat.Operator;
import com.gitlab.aecsocket.sokol.core.stat.Stat;

import java.util.Map;

import static com.gitlab.aecsocket.sokol.core.stat.Operator.*;

/**
 * A stat type which stores a string.
 */
public final class StringStat extends Stat<String> {
    public static final Operator<String> OP_SET = op("=", c -> c.arg(0), String.class);
    public static final Operator<String> OP_ADD = op("+", c -> c.base().orElse("") + c.arg(0), String.class);

    public static final Map<String, Operator<String>> OPERATORS = ops(OP_SET, OP_ADD);

    private StringStat(String key) { super(key); }
    public static StringStat stringStat(String key) { return new StringStat(key); }

    @Override public Map<String, Operator<String>> operators() { return OPERATORS; }
    @Override public Operator<String> defaultOperator() { return OP_SET; }
}
