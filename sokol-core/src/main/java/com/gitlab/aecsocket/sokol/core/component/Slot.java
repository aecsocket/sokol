package com.gitlab.aecsocket.sokol.core.component;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface Slot {
    Collection<String> tags();
    boolean tagged(String tag);

    @NotNull String key();
    @NotNull Component parent();
}
