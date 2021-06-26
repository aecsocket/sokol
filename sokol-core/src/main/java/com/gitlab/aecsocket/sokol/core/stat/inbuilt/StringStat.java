package com.gitlab.aecsocket.sokol.core.stat.inbuilt;

import com.gitlab.aecsocket.sokol.core.stat.BasicStat;
import io.leangen.geantyref.TypeToken;

/**
 * A stat type which stores a string.
 */
public class StringStat extends BasicStat<String> {
    public StringStat(String def) {
        super(new TypeToken<String>() {}, def, (a, b) -> b, v -> v);
    }

    public StringStat() {
        this(null);
    }
}
