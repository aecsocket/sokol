package com.github.aecsocket.sokol.core.stat;

import java.util.Collections;
import java.util.Map;

/* package */ final class EmptyStatTypes implements StatTypes {
    static final EmptyStatTypes INSTANCE = new EmptyStatTypes();

    private EmptyStatTypes() {}

    @Override
    public Map<String, Stat<?>> map() {
        return Collections.emptyMap();
    }
}
