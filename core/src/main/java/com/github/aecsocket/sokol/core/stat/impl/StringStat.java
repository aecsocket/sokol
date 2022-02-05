package com.github.aecsocket.sokol.core.stat.impl;

import com.github.aecsocket.sokol.core.stat.Stat;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.github.aecsocket.minecommons.core.serializers.Serializers.require;

public final class StringStat extends Stat<String> {
    public record Set(String val) implements Op.Initial<String>, Op.Discards {
        @Override public String first() { return val; }
    }
    public record Add(String val) implements Op.Initial<String> {
        @Override public String compute(String cur) { return cur + val; }
        @Override public String first() { return compute(""); }
    }

    private static final OpTypes<String> OP_TYPES = Stat.<String>buildOpTypes()
            .setDefault("=", (type, node, args) -> new Set(require(args[0], String.class)), "value")
            .set("+", (type, node, args) -> new Add(require(args[0], String.class)), "value")
            .build();

    private StringStat(String key, @Nullable String def) {
        super(key, def);
    }

    public static StringStat stringStat(String key, String def) {
        return new StringStat(key, def);
    }

    public static StringStat stringStat(String key) {
        return new StringStat(key, null);
    }

    @Override public OpTypes<String> opTypes() { return OP_TYPES; }
}
