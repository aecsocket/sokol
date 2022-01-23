package com.github.aecsocket.sokol.core.api;

import java.util.Set;

public interface NodeSlot {
    Component parent();
    String key();

    Set<String> tags();
    boolean tagged(String tag);

    <N extends BaseNode.Scoped<N, ?>> void compatible(N target, N parent) throws IncompatibleException;
}
