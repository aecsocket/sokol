package com.github.aecsocket.sokol.core;

import java.util.Set;

public interface NodeSlot {
    String REQUIRED = "required";

    SokolComponent parent();
    String key();

    Set<String> tags();
    boolean tagged(String key);

    boolean required();

    <N extends SokolNode> void compatible(N target, N parent) throws IncompatibleException;

    interface Scoped<
        S extends Scoped<S, C>,
        C extends SokolComponent
    > extends NodeSlot {
        @Override C parent();
    }
}
