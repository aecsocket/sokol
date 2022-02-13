package com.github.aecsocket.sokol.core.stat.impl;

import com.github.aecsocket.minecommons.core.i18n.I18N;
import com.github.aecsocket.sokol.core.stat.Stat;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Locale;

import static com.github.aecsocket.minecommons.core.serializers.Serializers.require;
import static net.kyori.adventure.text.Component.*;

public final class StringStat extends Stat<String> {
    public record Set(String value) implements Op.Initial<String>, Op.Discards {
        @Override public String first() { return value; }
        @Override
        public Component render(I18N i18n, Locale locale) {
            return i18n.line(locale, STAT_TYPE_STRING_SET,
                c -> c.of("value", () -> text(value)));
        }
    }
    public record Add(String value) implements Op.Initial<String> {
        @Override public String compute(String cur) { return cur + value; }
        @Override public String first() { return compute(""); }
        @Override
        public Component render(I18N i18n, Locale locale) {
            return i18n.line(locale, STAT_TYPE_STRING_ADD,
                c -> c.of("value", () -> text(value)));
        }
    }

    public static final String
        STAT_TYPE_STRING_SET = "stat_type.string.set",
        STAT_TYPE_STRING_ADD = "stat_type.string.add";
    private static final OpTypes<String> OP_TYPES = Stat.<String>buildOpTypes()
        .setDefault("=", (type, node, args) -> new Set(require(args[0], String.class)), "value")
        .set("+", (type, node, args) -> new Add(require(args[0], String.class)), "value")
        .build();

    private StringStat(String key, @Nullable String def) {
        super(key, def);
    }

    public static StringStat stat(String key, String def) {
        return new StringStat(key, def);
    }

    public static StringStat stat(String key) {
        return new StringStat(key, null);
    }

    @Override public OpTypes<String> opTypes() { return OP_TYPES; }

    @Override
    public Component renderValue(I18N i18n, Locale locale, String value) {
        return Component.text(value);
    }
}
