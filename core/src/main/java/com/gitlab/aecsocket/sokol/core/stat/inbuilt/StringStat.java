package com.gitlab.aecsocket.sokol.core.stat.inbuilt;

import com.gitlab.aecsocket.sokol.core.stat.BasicStat;
import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A stat type which stores a string.
 */
public class StringStat extends BasicStat<String> {
    private StringStat(@Nullable String def) {
        super(new TypeToken<>() {}, def, (a, b) -> b, v -> v);
    }

    public static StringStat stringStat(@Nullable String def) { return new StringStat(def); }
    public static StringStat stringStat() { return new StringStat(null); }
}
