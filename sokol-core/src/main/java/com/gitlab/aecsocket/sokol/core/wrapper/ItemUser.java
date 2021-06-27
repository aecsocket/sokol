package com.gitlab.aecsocket.sokol.core.wrapper;

import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Vector3;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public interface ItemUser {
    @NotNull Vector3 position();
    @NotNull Vector3 direction();
    @NotNull Locale locale();
}
