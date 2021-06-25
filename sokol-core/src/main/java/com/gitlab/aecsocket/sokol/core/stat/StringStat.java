package com.gitlab.aecsocket.sokol.core.stat;

import io.leangen.geantyref.TypeToken;

public class StringStat extends BasicStat<String> {
    public StringStat(String def) {
        super(new TypeToken<String>() {}, def, (a, b) -> b, v -> v);
    }

    public StringStat() {
        this(null);
    }
}
